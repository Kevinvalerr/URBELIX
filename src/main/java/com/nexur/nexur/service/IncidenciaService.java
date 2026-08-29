package com.nexur.nexur.service;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.EstadoIncidencia;
import com.nexur.nexur.model.Incidencia;
import com.nexur.nexur.model.IncidenciaAdjunto;
import com.nexur.nexur.model.IncidenciaComentario;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.repository.ApartamentoRepository;
import com.nexur.nexur.repository.IncidenciaRepository;
import com.nexur.nexur.repository.IncidenciaComentarioRepository;
import com.nexur.nexur.repository.IncidenciaAdjuntoRepository;
import com.nexur.nexur.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class IncidenciaService {

    private final IncidenciaRepository incidenciaRepository;
    private final ApartamentoRepository apartamentoRepository;
    private final IncidenciaComentarioRepository comentarioRepository;
    private final IncidenciaAdjuntoRepository adjuntoRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;
    private final ArchivoStorageService archivoStorageService;

    public IncidenciaService(IncidenciaRepository incidenciaRepository,
                             ApartamentoRepository apartamentoRepository,
                             IncidenciaComentarioRepository comentarioRepository,
                             IncidenciaAdjuntoRepository adjuntoRepository,
                             UsuarioRepository usuarioRepository,
                             NotificacionService notificacionService,
                             ArchivoStorageService archivoStorageService) {
        this.incidenciaRepository = incidenciaRepository;
        this.apartamentoRepository = apartamentoRepository;
        this.comentarioRepository = comentarioRepository;
        this.adjuntoRepository = adjuntoRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacionService = notificacionService;
        this.archivoStorageService = archivoStorageService;
    }

    @Transactional(readOnly = true)
    public List<Incidencia> listarTodas() {
        List<Incidencia> incidencias = incidenciaRepository.findAllByOrderByCreadoEnDesc();
        inicializarRelacionesDeListado(incidencias);
        return incidencias;
    }

    @Transactional(readOnly = true)
    public List<Incidencia> listarPorResidente(Long residenteId) {
        List<Incidencia> incidencias = incidenciaRepository.findByResidenteIdOrderByCreadoEnDesc(residenteId);
        inicializarRelacionesDeListado(incidencias);
        return incidencias;
    }

    private void inicializarRelacionesDeListado(List<Incidencia> incidencias) {
        incidencias.forEach(incidencia -> {
            incidencia.getComentarios().size();
            incidencia.getAdjuntos().size();
        });
    }

    public Incidencia buscarPorId(Long id) {
        return incidenciaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Incidencia no encontrada"));
    }

    @Transactional
    public Incidencia crear(Incidencia incidencia, Residente residente) {
        if (residente == null || residente.getId() == null) {
            throw new IllegalArgumentException("No se encontró el perfil del residente");
        }
        if (!StringUtils.hasText(incidencia.getTipo())) {
            incidencia.setTipo("GENERAL");
        }
        incidencia.setResidente(residente);
        incidencia.setApartamento(residente.getApartamento());
        incidencia.setEstado(EstadoIncidencia.ABIERTA);
        incidencia.setCreadoEn(LocalDateTime.now());
        incidencia.setActualizadoEn(null);
        return incidenciaRepository.save(incidencia);
    }

    @Transactional
    public void actualizarEstado(Long id, EstadoIncidencia estado, String respuesta, String autorEmail) {
        if (estado == null) {
            throw new IllegalArgumentException("El estado es obligatorio");
        }
        Incidencia incidencia = buscarPorId(id);
        incidencia.setEstado(estado);
        incidencia.setRespuesta(StringUtils.hasText(respuesta) ? respuesta.trim() : null);
        incidencia.setActualizadoEn(LocalDateTime.now());
        incidenciaRepository.save(incidencia);
        if (incidencia.getResidente() != null && incidencia.getResidente().getUsuario() != null) {
            notificacionService.crear(incidencia.getResidente().getUsuario(), "Actualización de incidencia",
                    "La incidencia '" + incidencia.getAsunto() + "' fue actualizada a " + estado.name() + ".",
                    "/incidencias");
        }
    }

    @Transactional
    public IncidenciaComentario agregarComentario(Long incidenciaId, String contenido,
                                                   String autorEmail, String autorNombre,
                                                   boolean esAdmin) {
        if (!StringUtils.hasText(contenido)) {
            throw new IllegalArgumentException("El comentario no puede estar vacío");
        }
        Incidencia incidencia = buscarPorId(incidenciaId);
        if (!esAdmin || autorEmail == null) {
            if (incidencia.getResidente() == null || incidencia.getResidente().getUsuario() == null
                    || !autorEmail.equalsIgnoreCase(incidencia.getResidente().getUsuario().getEmail())) {
                throw new IllegalArgumentException("No puede comentar esta incidencia");
            }
        }
        IncidenciaComentario comentario = new IncidenciaComentario();
        comentario.setContenido(contenido.trim());
        comentario.setAutorEmail(autorEmail);
        comentario.setAutorNombre(StringUtils.hasText(autorNombre) ? autorNombre.trim() : autorEmail);
        comentario.setCreadoEn(LocalDateTime.now());
        comentario.setIncidencia(incidencia);
        incidencia.setActualizadoEn(LocalDateTime.now());
        IncidenciaComentario guardado = comentarioRepository.save(comentario);
        if (esAdmin && incidencia.getResidente() != null && incidencia.getResidente().getUsuario() != null) {
            notificacionService.crear(incidencia.getResidente().getUsuario(), "Nuevo comentario en tu incidencia",
                    "Administración agregó un comentario en '" + incidencia.getAsunto() + "'.", "/incidencias");
        } else {
            usuarioRepository.findByRolAndActivoTrue(com.nexur.nexur.model.Rol.ADMIN).forEach(admin ->
                    notificacionService.crear(admin, "Nuevo comentario de residente",
                            "Se agregó un comentario en la incidencia '" + incidencia.getAsunto() + "'.", "/incidencias"));
        }
        return guardado;
    }

    @Transactional
    public IncidenciaAdjunto agregarAdjunto(Long incidenciaId, MultipartFile archivo,
                                            String autorEmail, boolean esAdmin) {
        Incidencia incidencia = buscarPorId(incidenciaId);
        validarAcceso(incidencia, autorEmail, esAdmin);

        String nombreInterno = archivoStorageService.guardar(archivo);
        try {
            IncidenciaAdjunto adjunto = new IncidenciaAdjunto();
            adjunto.setNombreOriginal(nombreOriginalSeguro(archivo.getOriginalFilename()));
            adjunto.setNombreInterno(nombreInterno);
            adjunto.setTipoContenido(archivo.getContentType());
            adjunto.setTamano(archivo.getSize());
            adjunto.setCargadoPor(autorEmail);
            adjunto.setCreadoEn(LocalDateTime.now());
            adjunto.setIncidencia(incidencia);
            IncidenciaAdjunto guardado = adjuntoRepository.save(adjunto);
            incidencia.setActualizadoEn(LocalDateTime.now());
            incidenciaRepository.save(incidencia);
            notificarNuevoAdjunto(incidencia, esAdmin);
            return guardado;
        } catch (RuntimeException exception) {
            archivoStorageService.eliminar(nombreInterno);
            throw exception;
        }
    }

    public IncidenciaAdjunto buscarAdjunto(Long incidenciaId, Long adjuntoId,
                                           String email, boolean esAdmin) {
        Incidencia incidencia = buscarPorId(incidenciaId);
        validarAcceso(incidencia, email, esAdmin);
        return adjuntoRepository.findByIdAndIncidenciaId(adjuntoId, incidenciaId)
                .orElseThrow(() -> new IllegalArgumentException("Evidencia no encontrada"));
    }

    private void validarAcceso(Incidencia incidencia, String email, boolean esAdmin) {
        if (esAdmin) {
            return;
        }
        if (incidencia.getResidente() == null || incidencia.getResidente().getUsuario() == null
                || email == null || !email.equalsIgnoreCase(incidencia.getResidente().getUsuario().getEmail())) {
            throw new IllegalArgumentException("No puede acceder a esta incidencia");
        }
    }

    private void notificarNuevoAdjunto(Incidencia incidencia, boolean esAdmin) {
        if (esAdmin && incidencia.getResidente() != null && incidencia.getResidente().getUsuario() != null) {
            notificacionService.crear(incidencia.getResidente().getUsuario(), "Nueva evidencia en tu incidencia",
                    "Administración adjuntó un archivo a '" + incidencia.getAsunto() + "'.", "/incidencias");
        } else {
            usuarioRepository.findByRolAndActivoTrue(com.nexur.nexur.model.Rol.ADMIN).forEach(admin ->
                    notificacionService.crear(admin, "Nueva evidencia de residente",
                            "Se adjuntó un archivo a la incidencia '" + incidencia.getAsunto() + "'.", "/incidencias"));
        }
    }

    private String nombreOriginalSeguro(String original) {
        String nombre = StringUtils.cleanPath(StringUtils.hasText(original) ? original : "evidencia");
        nombre = nombre.replace('\\', '/');
        int ultimaBarra = nombre.lastIndexOf('/');
        if (ultimaBarra >= 0) {
            nombre = nombre.substring(ultimaBarra + 1);
        }
        return nombre.length() > 255 ? nombre.substring(0, 255) : nombre;
    }

    public long contarAbiertas() {
        return incidenciaRepository.countAbiertas();
    }
}
