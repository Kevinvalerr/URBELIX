package com.nexur.nexur.service;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.model.Rol;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.repository.ApartamentoRepository;
import com.nexur.nexur.repository.ResidenteRepository;
import com.nexur.nexur.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ResidenteRepository residenteRepository;
    @Mock
    private ApartamentoRepository apartamentoRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void rechazaRegistroConCodigoResidencialIncorrecto() {
        Apartamento apartamento = new Apartamento();
        apartamento.setCodigoRegistro("URB-101-A");
        when(apartamentoRepository.findByNumero("101")).thenReturn(Optional.of(apartamento));

        UsuarioService service = new UsuarioService(usuarioRepository, residenteRepository,
                apartamentoRepository, passwordEncoder);

        assertThrows(IllegalArgumentException.class, () -> service.crearUsuarioConResidente(
                "Ana", "ana@example.com", "Segura123!", "12345678", "3001234567",
                "101", "URB-INVALIDO"));
    }

    @Test
    void puedeCrearPorteriaSinApartamento() {
        when(usuarioRepository.existsByEmail("porteria@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Segura123!")).thenReturn("hash");
        when(usuarioRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioService service = new UsuarioService(usuarioRepository, residenteRepository,
                apartamentoRepository, passwordEncoder);

        var usuario = service.crearUsuario("Porteria", "porteria@example.com", "Segura123!",
                Rol.PORTERIA, null, null, null);

        assertEquals(Rol.PORTERIA, usuario.getRol());
        assertEquals(true, usuario.isDebeCambiarPassword());
    }

    @Test
    void rechazaContrasenaQueNoCumpleLaPolitica() {
        UsuarioService service = new UsuarioService(usuarioRepository, residenteRepository,
                apartamentoRepository, passwordEncoder);

        assertThrows(IllegalArgumentException.class,
                () -> service.cambiarPassword(new com.nexur.nexur.model.Usuario(), "debil"));
    }

    @Test
    void rechazaDocumentoCortoEnRegistroResidencial() {
        UsuarioService service = new UsuarioService(usuarioRepository, residenteRepository,
                apartamentoRepository, passwordEncoder);

        assertThrows(IllegalArgumentException.class, () -> service.crearUsuarioConResidente(
                "Ana", "ana@example.com", "Segura123!", "1234567", "3001234567",
                "101"));
    }

    @Test
    void rechazaTelefonoConFormatoNoNumericoEnRegistroResidencial() {
        UsuarioService service = new UsuarioService(usuarioRepository, residenteRepository,
                apartamentoRepository, passwordEncoder);

        assertThrows(IllegalArgumentException.class, () -> service.crearUsuarioConResidente(
                "Ana", "ana@example.com", "Segura123!", "12345678", "+573001234567",
                "101"));
    }

    @Test
    void normalizaCorreoYCodigoResidencialAntesDeCrearCuenta() {
        Apartamento apartamento = new Apartamento();
        apartamento.setCodigoRegistro("URB-101-A");
        when(apartamentoRepository.findByNumero("101")).thenReturn(Optional.of(apartamento));
        when(usuarioRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(residenteRepository.existsByDocumento("12345678")).thenReturn(false);
        when(passwordEncoder.encode("Segura123!")).thenReturn("hash");
        when(usuarioRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioService service = new UsuarioService(usuarioRepository, residenteRepository,
                apartamentoRepository, passwordEncoder);

        var usuario = service.crearUsuarioConResidente(
                " Ana ", " ANA@EXAMPLE.COM ", "Segura123!", "12345678", "3001234567",
                "101", " urb-101-a ");

        assertEquals("ana@example.com", usuario.getEmail());
        assertEquals("Ana", usuario.getNombre());
        assertEquals(true, usuario.isDebeCambiarPassword());
    }

    @Test
    void noPermiteDesactivarLaCuentaDelAdministradorActual() {
        var admin = new com.nexur.nexur.model.Usuario();
        admin.setId(1L);
        admin.setEmail("admin@example.com");
        admin.setRol(Rol.ADMIN);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));

        UsuarioService service = new UsuarioService(usuarioRepository, residenteRepository,
                apartamentoRepository, passwordEncoder);

        assertThrows(IllegalArgumentException.class,
                () -> service.cambiarEstado(1L, false, "ADMIN@EXAMPLE.COM"));
    }

    @Test
    void noPermiteDesactivarElUltimoAdministradorActivo() {
        var admin = new com.nexur.nexur.model.Usuario();
        admin.setId(2L);
        admin.setEmail("otro-admin@example.com");
        admin.setRol(Rol.ADMIN);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN)).thenReturn(1L);

        UsuarioService service = new UsuarioService(usuarioRepository, residenteRepository,
                apartamentoRepository, passwordEncoder);

        assertThrows(IllegalArgumentException.class,
                () -> service.cambiarEstado(2L, false, "admin@example.com"));
    }

    @Test
    void residentePuedeActualizarSusDatosDeContacto() {
        Usuario usuario = new Usuario();
        usuario.setEmail("residente@example.com");
        usuario.setRol(Rol.RESIDENTE);
        Residente residente = new Residente();
        residente.setUsuario(usuario);
        when(usuarioRepository.findByEmail("residente@example.com")).thenReturn(Optional.of(usuario));
        when(residenteRepository.findByUsuarioEmail("residente@example.com")).thenReturn(Optional.of(residente));
        when(usuarioRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioService service = new UsuarioService(usuarioRepository, residenteRepository,
                apartamentoRepository, passwordEncoder);
        Usuario actualizado = service.actualizarPerfilPropio("residente@example.com", " Ana Pérez ", "3001234567");

        assertEquals("Ana Pérez", actualizado.getNombre());
        assertEquals("Ana Pérez", residente.getNombre());
        assertEquals("3001234567", residente.getTelefono());
    }

    @Test
    void rechazaTelefonoInvalidoAlActualizarPerfil() {
        Usuario usuario = new Usuario();
        usuario.setEmail("residente@example.com");
        usuario.setRol(Rol.RESIDENTE);
        when(usuarioRepository.findByEmail("residente@example.com")).thenReturn(Optional.of(usuario));

        UsuarioService service = new UsuarioService(usuarioRepository, residenteRepository,
                apartamentoRepository, passwordEncoder);

        assertThrows(IllegalArgumentException.class,
                () -> service.actualizarPerfilPropio("residente@example.com", "Ana", "123"));
    }

    @Test
    void buscaUsuariosPorCorreoNormalizado() {
        Usuario usuario = new Usuario();
        usuario.setEmail("residente@example.com");
        when(usuarioRepository.findByEmail("residente@example.com")).thenReturn(Optional.of(usuario));

        UsuarioService service = new UsuarioService(usuarioRepository, residenteRepository,
                apartamentoRepository, passwordEncoder);
        assertSame(usuario, service.buscarPorEmail(" Residente@Example.com "));
        verify(usuarioRepository).findByEmail("residente@example.com");
    }

    @Test
    void cambiarPasswordLiberaLaCuentaDelCambioInicial() {
        Usuario usuario = new Usuario();
        usuario.setDebeCambiarPassword(true);
        when(passwordEncoder.encode("Segura123!")).thenReturn("hash");
        when(usuarioRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioService service = new UsuarioService(usuarioRepository, residenteRepository,
                apartamentoRepository, passwordEncoder);
        service.cambiarPassword(usuario, "Segura123!");

        assertEquals(false, usuario.isDebeCambiarPassword());
    }

    @Test
    void noPermiteDegradarAlUltimoAdministradorDesdeEdicion() {
        Usuario admin = new Usuario();
        admin.setId(10L);
        admin.setEmail("admin@example.com");
        admin.setRol(Rol.ADMIN);
        admin.setActivo(true);
        Usuario cambios = new Usuario();
        cambios.setId(10L);
        cambios.setRol(Rol.PORTERIA);
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(admin));
        when(usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN)).thenReturn(1L);

        UsuarioService service = new UsuarioService(usuarioRepository, residenteRepository,
                apartamentoRepository, passwordEncoder);

        assertThrows(IllegalArgumentException.class,
                () -> service.guardarUsuarioActualizado(cambios));
    }

    @Test
    void rechazaEmailDuplicadoAlEditarUsuario() {
        Usuario existente = new Usuario();
        existente.setId(11L);
        existente.setEmail("uno@example.com");
        existente.setRol(Rol.PORTERIA);
        Usuario cambios = new Usuario();
        cambios.setId(11L);
        cambios.setEmail(" DOS@EXAMPLE.COM ");
        when(usuarioRepository.findById(11L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.existsByEmail("dos@example.com")).thenReturn(true);

        UsuarioService service = new UsuarioService(usuarioRepository, residenteRepository,
                apartamentoRepository, passwordEncoder);

        assertThrows(IllegalArgumentException.class,
                () -> service.guardarUsuarioActualizado(cambios));
    }

    @Test
    void sincronizaNombreDelResidenteAlEditarSuUsuario() {
        Usuario existente = new Usuario();
        existente.setId(12L);
        existente.setEmail("residente@example.com");
        existente.setRol(Rol.RESIDENTE);
        Residente residente = new Residente();
        residente.setNombre("Nombre anterior");
        existente.setResidente(residente);

        Usuario cambios = new Usuario();
        cambios.setId(12L);
        cambios.setNombre("  Nuevo nombre  ");

        when(usuarioRepository.findById(12L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(residenteRepository.save(any(Residente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioService service = new UsuarioService(usuarioRepository, residenteRepository,
                apartamentoRepository, passwordEncoder);

        service.guardarUsuarioActualizado(cambios);

        assertEquals("Nuevo nombre", existente.getNombre());
        assertEquals("Nuevo nombre", residente.getNombre());
    }
}
