package com.nexur.nexur.service;

import com.nexur.nexur.model.Notificacion;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.repository.NotificacionRepository;
import com.nexur.nexur.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock
    private NotificacionRepository notificacionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CorreoNotificacionService correoNotificacionService;

    @Test
    void creaNotificacionParaUnUsuarioPersistido() {
        Usuario usuario = new Usuario();
        usuario.setId(4L);
        when(usuarioRepository.findById(4L)).thenReturn(Optional.of(usuario));
        when(notificacionRepository.save(any(Notificacion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificacionService service = new NotificacionService(notificacionRepository, usuarioRepository,
                correoNotificacionService);
        service.crear(usuario, "Aviso", "Mensaje", "/avisos");

        verify(notificacionRepository).save(any(Notificacion.class));
    }

    @Test
    void noPermiteMarcarNotificacionDeOtroUsuario() {
        Usuario propietario = new Usuario();
        propietario.setEmail("dueno@example.com");
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuario(propietario);
        when(notificacionRepository.findById(1L)).thenReturn(Optional.of(notificacion));

        NotificacionService service = new NotificacionService(notificacionRepository, usuarioRepository,
                correoNotificacionService);

        assertThrows(IllegalArgumentException.class,
                () -> service.marcarLeida(1L, "otro@example.com"));
    }

    @Test
    void contadorUsaElUsuarioAunqueAunNoTengaNotificaciones() {
        Usuario usuario = new Usuario();
        usuario.setId(8L);
        when(usuarioRepository.findByEmail("residente@example.com")).thenReturn(Optional.of(usuario));
        when(notificacionRepository.countByUsuarioIdAndLeidaFalse(8L)).thenReturn(0L);

        NotificacionService service = new NotificacionService(notificacionRepository, usuarioRepository,
                correoNotificacionService);

        assertEquals(0L, service.contarNoLeidas("residente@example.com"));
    }

    @Test
    void marcaTodasLasNotificacionesDelUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(8L);
        when(usuarioRepository.findByEmail("residente@example.com")).thenReturn(Optional.of(usuario));
        when(notificacionRepository.marcarTodasLeidas(8L)).thenReturn(3);

        NotificacionService service = new NotificacionService(notificacionRepository, usuarioRepository,
                correoNotificacionService);

        assertEquals(3, service.marcarTodasLeidas("residente@example.com"));
        verify(notificacionRepository).marcarTodasLeidas(8L);
    }
}
