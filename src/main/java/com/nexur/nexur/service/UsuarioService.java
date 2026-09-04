package com.nexur.nexur.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.model.Rol;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.repository.ApartamentoRepository;
import com.nexur.nexur.repository.ResidenteRepository;
import com.nexur.nexur.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ResidenteRepository residenteRepository;
    private final ApartamentoRepository apartamentoRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          ResidenteRepository residenteRepository,
                          ApartamentoRepository apartamentoRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.residenteRepository = residenteRepository;
        this.apartamentoRepository = apartamentoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    //  MÉTODO NUEVO (CLAVE)
    @Transactional
    public Usuario crearUsuarioConResidente(
            String nombre,
            String email,
            String password,
            String documento,
            String telefono,
            String numeroApartamento
    ) {

        validarPassword(password);
        String nombreNormalizado = textoObligatorio(nombre, "El nombre es obligatorio");
        String emailNormalizado = normalizarEmail(email);
        String documentoNormalizado = normalizarDocumento(documento);
        String telefonoNormalizado = normalizarTelefono(telefono);
        String apartamentoNormalizado = textoObligatorio(numeroApartamento, "El apartamento es obligatorio");

        // Validar email único
        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new RuntimeException("El email ya está en uso");
        }

        // Validar documento único
        if (residenteRepository.existsByDocumento(documentoNormalizado)) {
            throw new RuntimeException("El documento ya está registrado");
        }

        // Buscar apartamento
        Apartamento apartamento = apartamentoRepository
                .findByNumero(apartamentoNormalizado)
                .orElseThrow(() -> new RuntimeException("Apartamento no encontrado"));

        // Crear usuario
        Usuario usuario = new Usuario();
        usuario.setNombre(nombreNormalizado);
        usuario.setEmail(emailNormalizado);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setRol(Rol.RESIDENTE);
        usuario.setDebeCambiarPassword(true);

        // Crear residente
        Residente residente = new Residente();
        residente.setNombre(nombreNormalizado);
        residente.setDocumento(documentoNormalizado);
        residente.setTelefono(telefonoNormalizado);
        residente.setApartamento(apartamento);

        // Relación bidireccional
        residente.setUsuario(usuario);
        usuario.setResidente(residente);

        // Guardar todo (cascade)
        return usuarioRepository.save(usuario);
    }

    //  MÉTODO ORIGINAL (NO TOCAR)
    public Usuario crearUsuarioConResidente(String nombre,
                                            String email,
                                            String password,
                                            String documento,
                                            String telefono,
                                            String numeroApartamento,
                                            String codigoRegistro) {
        String apartamentoNormalizado = textoObligatorio(numeroApartamento, "El apartamento es obligatorio");
        String codigoNormalizado = textoObligatorio(codigoRegistro, "El codigo de registro es obligatorio");
        Apartamento apartamento = apartamentoRepository.findByNumero(apartamentoNormalizado)
                .orElseThrow(() -> new IllegalArgumentException("Apartamento no encontrado"));
        if (!StringUtils.hasText(apartamento.getCodigoRegistro())
                || !codigoNormalizado.equalsIgnoreCase(apartamento.getCodigoRegistro().trim())) {
            throw new IllegalArgumentException("El código de registro no corresponde al apartamento");
        }
        return crearUsuarioConResidente(nombre, email, password, documento, telefono, apartamentoNormalizado);
    }

    @Transactional
    public CuentaImportada crearCuentaResidenteImportada(String nombre,
                                                          String email,
                                                          String documento,
                                                          String telefono,
                                                          String numeroApartamento,
                                                          String codigoRegistro) {
        String passwordTemporal = generarPasswordTemporal();
        Usuario usuario = crearUsuarioConResidente(nombre, email, passwordTemporal,
                documento, telefono, numeroApartamento, codigoRegistro);
        return new CuentaImportada(usuario, passwordTemporal);
    }

    @Transactional
    public Usuario crearUsuario(String nombre,
                                String email,
                                String password,
                                Rol rol,
                                String documento,
                                String telefono,
                                String numeroApartamento) {
        validarPassword(password);
        String nombreNormalizado = textoObligatorio(nombre, "El nombre es obligatorio");
        String emailNormalizado = normalizarEmail(email);
        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new IllegalArgumentException("El email ya está en uso");
        }
        if (rol == null) {
            rol = Rol.RESIDENTE;
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(nombreNormalizado);
        usuario.setEmail(emailNormalizado);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setRol(rol);
        usuario.setDebeCambiarPassword(true);

        if (rol != Rol.RESIDENTE) {
            return usuarioRepository.save(usuario);
        }
        if (!StringUtils.hasText(documento) || !StringUtils.hasText(telefono)
                || !StringUtils.hasText(numeroApartamento)) {
            throw new IllegalArgumentException("Para un residente son obligatorios documento, teléfono y apartamento");
        }
        String documentoNormalizado = normalizarDocumento(documento);
        String telefonoNormalizado = normalizarTelefono(telefono);
        String apartamentoNormalizado = numeroApartamento.trim();
        if (residenteRepository.existsByDocumento(documentoNormalizado)) {
            throw new IllegalArgumentException("El documento ya está registrado");
        }
        Apartamento apartamento = apartamentoRepository.findByNumero(apartamentoNormalizado)
                .orElseThrow(() -> new IllegalArgumentException("Apartamento no encontrado"));

        Residente residente = new Residente();
        residente.setNombre(nombreNormalizado);
        residente.setDocumento(documentoNormalizado);
        residente.setTelefono(telefonoNormalizado);
        residente.setApartamento(apartamento);
        residente.setUsuario(usuario);
        usuario.setResidente(residente);
        return usuarioRepository.save(usuario);
    }

    public Usuario guardarUsuario(Usuario usuario) {
        validarPassword(usuario.getPassword());
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        if (usuario.getId() == null) {
            usuario.setDebeCambiarPassword(true);
        }

        if (usuario.getRol() == null) {
            usuario.setRol(Rol.RESIDENTE);
        }

        return usuarioRepository.save(usuario);
    }

    //  ACTUALIZAR USUARIO
    @Transactional
    public Usuario guardarUsuarioActualizado(Usuario usuario) {

        if (usuario == null) {
            return null;
        }

        if (usuario.getId() == null) {
            return guardarUsuario(usuario);
        }

        Usuario existente = usuarioRepository.findById(usuario.getId()).orElse(usuario);

        if (StringUtils.hasText(usuario.getPassword())) {
            validarPassword(usuario.getPassword());
            existente.setPassword(passwordEncoder.encode(usuario.getPassword()));
            existente.setDebeCambiarPassword(true);
        }

        if (StringUtils.hasText(usuario.getEmail())) {
            String emailNormalizado = normalizarEmail(usuario.getEmail());
            if (!emailNormalizado.equalsIgnoreCase(existente.getEmail())
                    && usuarioRepository.existsByEmail(emailNormalizado)) {
                throw new IllegalArgumentException("El email ya está en uso");
            }
            existente.setEmail(emailNormalizado);
        }

        if (StringUtils.hasText(usuario.getNombre())) {
            String nombreNormalizado = textoObligatorio(usuario.getNombre(), "El nombre es obligatorio");
            existente.setNombre(nombreNormalizado);
            if (existente.getResidente() != null) {
                existente.getResidente().setNombre(nombreNormalizado);
                residenteRepository.save(existente.getResidente());
            }
        }

        if (usuario.getRol() != null && usuario.getRol() != existente.getRol()) {
            validarCambioDeRol(existente, usuario.getRol());
            existente.setRol(usuario.getRol());
        }

        return usuarioRepository.save(existente);
    }

    private void validarCambioDeRol(Usuario existente, Rol nuevoRol) {
        if ((existente.getRol() == Rol.RESIDENTE || nuevoRol == Rol.RESIDENTE)
                && existente.getRol() != nuevoRol) {
            throw new IllegalArgumentException(
                    "El cambio entre residente y personal requiere actualizar también el perfil residencial");
        }
        if (existente.getRol() == Rol.ADMIN
                && existente.isActivo()
                && nuevoRol != Rol.ADMIN
                && usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN) <= 1) {
            throw new IllegalArgumentException("Debe existir al menos un administrador activo");
        }
    }

    //  OTROS MÉTODOS

    public boolean existePorEmail(String email) {
        return StringUtils.hasText(email) && usuarioRepository.existsByEmail(normalizarEmail(email));
    }

    public Usuario buscarPorEmail(String email) {
        return StringUtils.hasText(email)
                ? usuarioRepository.findByEmail(normalizarEmail(email)).orElse(null)
                : null;
    }

    @Transactional
    public Usuario actualizarPerfilPropio(String email, String nombre, String telefono) {
        String emailNormalizado = normalizarEmail(email);
        Usuario usuario = usuarioRepository.findByEmail(emailNormalizado)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        String nombreNormalizado = textoObligatorio(nombre, "El nombre es obligatorio");
        usuario.setNombre(nombreNormalizado);

        if (usuario.getRol() == Rol.RESIDENTE) {
            String telefonoNormalizado = textoObligatorio(telefono, "El teléfono es obligatorio");
            if (!telefonoNormalizado.matches("\\d{10,}")) {
                throw new IllegalArgumentException("El teléfono debe tener al menos 10 dígitos");
            }
            Residente residente = residenteRepository.findByUsuarioEmail(emailNormalizado)
                    .orElseThrow(() -> new IllegalArgumentException("No se encontró el perfil de residente"));
            residente.setNombre(nombreNormalizado);
            residente.setTelefono(telefonoNormalizado);
            residenteRepository.save(residente);
        }
        return usuarioRepository.save(usuario);
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

    @Transactional
    public Usuario cambiarEstado(Long id, boolean activo, String emailActor) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (!activo && usuario.getEmail() != null && usuario.getEmail().equalsIgnoreCase(emailActor)) {
            throw new IllegalArgumentException("No puede desactivar su propia cuenta");
        }
        if (!activo && usuario.getRol() == Rol.ADMIN
                && usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN) <= 1) {
            throw new IllegalArgumentException("Debe existir al menos un administrador activo");
        }
        usuario.setActivo(activo);
        return usuarioRepository.save(usuario);
    }

    public void cambiarPassword(Usuario usuario, String password) {
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }
        validarPassword(password);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setDebeCambiarPassword(false);
        usuarioRepository.save(usuario);
    }

    private void validarPassword(String password) {
        if (!StringUtils.hasText(password)
                || !password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$")) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener mínimo 8 caracteres, una mayúscula, una minúscula, un número y un símbolo");
        }
    }

    private String generarPasswordTemporal() {
        return "Ur" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "9!";
    }

    public record CuentaImportada(Usuario usuario, String passwordTemporal) {
    }

    private String normalizarEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("El email es obligatorio");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String textoObligatorio(String valor, String mensaje) {
        if (!StringUtils.hasText(valor)) {
            throw new IllegalArgumentException(mensaje);
        }
        return valor.trim();
    }

    private String normalizarDocumento(String documento) {
        String resultado = textoObligatorio(documento, "El documento es obligatorio");
        if (!resultado.matches("\\d{8,}")) {
            throw new IllegalArgumentException("El documento debe tener al menos 8 digitos y solo numeros");
        }
        return resultado;
    }

    private String normalizarTelefono(String telefono) {
        String resultado = textoObligatorio(telefono, "El telefono es obligatorio");
        if (!resultado.matches("\\d{10,}")) {
            throw new IllegalArgumentException("El telefono debe tener al menos 10 digitos y solo numeros");
        }
        return resultado;
    }
}
