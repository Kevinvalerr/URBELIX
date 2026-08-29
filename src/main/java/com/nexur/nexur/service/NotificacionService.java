package com.nexur.nexur.service;

import com.nexur.nexur.model.Notificacion;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.repository.NotificacionRepository;
import com.nexur.nexur.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final CorreoNotificacionService correoNotificacionService;

    public NotificacionService(NotificacionRepository notificacionRepository,
                               UsuarioRepository usuarioRepository,
                               CorreoNotificacionService correoNotificacionService) {
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.correoNotificacionService = correoNotificacionService;
    }

    @Transactional
    public void crear(Usuario usuario, String titulo, String mensaje, String enlace) {
        if (usuario == null || usuario.getId() == null) {
            return;
        }
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuario(usuario);
        notificacion.setTitulo(titulo);
        notificacion.setMensaje(mensaje);
        notificacion.setEnlace(enlace);
        notificacion.setCreadaEn(LocalDateTime.now());
        notificacionRepository.save(notificacion);
        correoNotificacionService.enviar(usuario, titulo, mensaje, enlace);
    }

    public List<Notificacion> listar(String email) {
        return notificacionRepository.findByUsuarioIdOrderByCreadaEnDesc(usuarioId(email));
    }

    public long contarNoLeidas(String email) {
        return notificacionRepository.countByUsuarioIdAndLeidaFalse(usuarioId(email));
    }

    @Transactional
    public void marcarLeida(Long id, String email) {
        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notificación no encontrada"));
        if (notificacion.getUsuario() == null || !email.equalsIgnoreCase(notificacion.getUsuario().getEmail())) {
            throw new IllegalArgumentException("No puede modificar esta notificación");
        }
        notificacion.setLeida(true);
        notificacionRepository.save(notificacion);
    }

    private Long usuarioId(String email) {
        return usuarioRepository.findByEmail(email).map(Usuario::getId).orElse(-1L);
    }
}
