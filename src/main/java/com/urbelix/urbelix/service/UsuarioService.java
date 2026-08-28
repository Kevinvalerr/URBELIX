package com.urbelix.urbelix.service;

import java.util.List;
import java.security.SecureRandom;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.urbelix.urbelix.model.Apartamento;
import com.urbelix.urbelix.model.Residente;
import com.urbelix.urbelix.model.Rol;
import com.urbelix.urbelix.model.Usuario;
import com.urbelix.urbelix.repository.ApartamentoRepository;
import com.urbelix.urbelix.repository.ResidenteRepository;
import com.urbelix.urbelix.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private static final String TEMPORARY_PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final ResidenteRepository residenteRepository;
    private final ApartamentoRepository apartamentoRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          ResidenteRepository residenteRepository,
                          ApartamentoRepository apartamentoRepository,
                          PasswordEncoder passwordEncoder,
                          AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.residenteRepository = residenteRepository;
        this.apartamentoRepository = apartamentoRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
    }

    //  MÉTODO NUEVO (CLAVE)
    @Transactional
    public Usuario crearUsuarioConResidente(
            String nombre,
            String email,
            String password,
            String documento,
            String telefono,
            String numeroApartamento,
            Rol rol
    ) {

        if (!StringUtils.hasText(nombre) || !StringUtils.hasText(email) || !StringUtils.hasText(password) || rol == null) {
            throw new IllegalArgumentException("Nombre, correo, contraseña y rol son obligatorios");
        }

        // Validar email único
        if (usuarioRepository.existsByEmail(email)) {
            throw new RuntimeException("El email ya está en uso");
        }

        // Validar documento único
        if (residenteRepository.existsByDocumento(documento)) {
            throw new RuntimeException("El documento ya está registrado");
        }

        // Crear usuario
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setRol(rol);

        if (rol != Rol.RESIDENTE) {
            return usuarioRepository.save(usuario);
        }

        if (!StringUtils.hasText(documento) || !StringUtils.hasText(telefono)
                || !StringUtils.hasText(numeroApartamento)) {
            throw new IllegalArgumentException("Documento, teléfono y apartamento son obligatorios para un residente");
        }

        Apartamento apartamento = apartamentoRepository
            .findByNumero(numeroApartamento)
            .orElseThrow(() -> new RuntimeException("Apartamento no encontrado"));

        // Crear residente
        Residente residente = new Residente();
        residente.setNombre(nombre);
        residente.setDocumento(documento);
        residente.setTelefono(telefono);
        residente.setApartamento(apartamento);

        // Relación bidireccional
        residente.setUsuario(usuario);
        usuario.setResidente(residente);

        // Guardar todo (cascade)
        return usuarioRepository.save(usuario);
    }

    //  MÉTODO ORIGINAL (NO TOCAR)
    @Transactional
    public Usuario guardarUsuario(Usuario usuario) {
        if (usuario == null || !StringUtils.hasText(usuario.getPassword())) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));

        if (usuario.getRol() == null) {
            usuario.setRol(Rol.RESIDENTE);
        }

        return usuarioRepository.save(usuario);
    }

    //  ACTUALIZAR USUARIO
    public Usuario guardarUsuarioActualizado(Usuario usuario) {

        if (usuario == null) {
            return null;
        }

        if (usuario.getId() == null) {
            return guardarUsuario(usuario);
        }

        Usuario existente = usuarioRepository.findById(usuario.getId()).orElse(usuario);

        if (StringUtils.hasText(usuario.getPassword())) {
            existente.setPassword(passwordEncoder.encode(usuario.getPassword()));
        }

        if (StringUtils.hasText(usuario.getEmail())) {
            existente.setEmail(usuario.getEmail());
        }

        if (StringUtils.hasText(usuario.getNombre())) {
            existente.setNombre(usuario.getNombre());
        }

        if (usuario.getRol() != null) {
            existente.setRol(usuario.getRol());
        }

        return usuarioRepository.save(existente);
    }

    @Transactional
    public String crearCuentaResidente(Residente residente) {
        if (residente == null || !StringUtils.hasText(residente.getCorreo())) {
            throw new IllegalArgumentException("El correo del residente es obligatorio");
        }
        String email = residente.getCorreo().trim().toLowerCase(java.util.Locale.ROOT);
        if (usuarioRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("El correo ya está registrado");
        }

        String passwordTemporal = generarPasswordTemporal();
        Usuario usuario = new Usuario();
        usuario.setNombre(residente.getNombre());
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(passwordTemporal));
        usuario.setRol(Rol.RESIDENTE);
        usuario.setDebeCambiarPassword(true);
        usuario.setResidente(residente);
        usuarioRepository.save(usuario);

        residente.setCorreo(email);
        residente.setUsuario(usuario);
        residenteRepository.save(residente);
        auditoriaService.registrar("CREAR_USUARIO", "RESIDENTES", "Usuario", usuario.getId(),
            "EXITO", "Cuenta RESIDENTE creada automáticamente");
        return passwordTemporal;
    }

    @Transactional
    public void cambiarPassword(String email, String passwordActual, String passwordNueva) {
        if (!StringUtils.hasText(passwordNueva) || passwordNueva.length() < 8) {
            throw new IllegalArgumentException("La nueva contraseña debe tener al menos 8 caracteres");
        }
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta");
        }
        usuario.setPassword(passwordEncoder.encode(passwordNueva));
        usuario.setDebeCambiarPassword(false);
        usuarioRepository.save(usuario);
        auditoriaService.registrar("CAMBIAR_PASSWORD", "SEGURIDAD", "Usuario", usuario.getId(),
            "EXITO", "Contraseña actualizada");
    }

    private String generarPasswordTemporal() {
        StringBuilder password = new StringBuilder(14);
        for (int index = 0; index < 14; index++) {
            password.append(TEMPORARY_PASSWORD_ALPHABET.charAt(
                    SECURE_RANDOM.nextInt(TEMPORARY_PASSWORD_ALPHABET.length())));
        }
        return password.toString();
    }

    //  OTROS MÉTODOS

    public boolean existePorEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }

    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email).orElse(null);
    }

    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id).orElse(null);
    }

    public void eliminarUsuario(Long id) {
        usuarioRepository.deleteById(id);
    }
}