package com.nexur.nexur.service;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.EstadoIncidencia;
import com.nexur.nexur.model.Incidencia;
import com.nexur.nexur.model.IncidenciaAdjunto;
import com.nexur.nexur.model.IncidenciaComentario;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.model.Rol;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.repository.ApartamentoRepository;
import com.nexur.nexur.repository.IncidenciaAdjuntoRepository;
import com.nexur.nexur.repository.IncidenciaComentarioRepository;
import com.nexur.nexur.repository.IncidenciaRepository;
import com.nexur.nexur.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidenciaServiceTest {
    @Mock private IncidenciaRepository incidenciaRepository;
    @Mock private ApartamentoRepository apartamentoRepository;
    @Mock private IncidenciaComentarioRepository comentarioRepository;
    @Mock private IncidenciaAdjuntoRepository adjuntoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private NotificacionService notificacionService;
    @Mock private ArchivoStorageService archivoStorageService;

    @Test
    void listaIncidenciasInicializandoRelacionesYCuentaAbiertas() {
        Incidencia incidencia = incidencia();
        when(incidenciaRepository.findAllByOrderByCreadoEnDesc()).thenReturn(List.of(incidencia));
        when(incidenciaRepository.countAbiertas()).thenReturn(3L);
        IncidenciaService service = service();

        assertEquals(1, service.listarTodas().size());
        assertEquals(3L, service.contarAbiertas());
        when(incidenciaRepository.findByResidenteIdOrderByCreadoEnDesc(4L)).thenReturn(List.of(incidencia));
        assertEquals(1, service.listarPorResidente(4L).size());
    }

    @Test
    void creaIncidenciaConTipoGeneralYRechazaResidenteInvalido() {
        Incidencia nueva = new Incidencia();
        nueva.setAsunto("Fuga");
        Residente residente = residente();
        when(incidenciaRepository.save(nueva)).thenReturn(nueva);

        Incidencia guardada = service().crear(nueva, residente);

        assertEquals("GENERAL", guardada.getTipo());
        assertEquals(EstadoIncidencia.ABIERTA, guardada.getEstado());
        assertEquals(residente, guardada.getResidente());
        assertThrows(IllegalArgumentException.class, () -> service().crear(new Incidencia(), null));
        residente.setId(null);
        assertThrows(IllegalArgumentException.class, () -> service().crear(new Incidencia(), residente));
    }

    @Test
    void actualizaEstadoYNotificaAlResidente() {
        Incidencia incidencia = incidencia();
        when(incidenciaRepository.findById(1L)).thenReturn(Optional.of(incidencia));

        service().actualizarEstado(1L, EstadoIncidencia.RESUELTA, "  Reparado  ", "admin@example.com");

        assertEquals(EstadoIncidencia.RESUELTA, incidencia.getEstado());
        assertEquals("Reparado", incidencia.getRespuesta());
        verify(incidenciaRepository).save(incidencia);
        verify(notificacionService).crear(any(Usuario.class), any(String.class), any(String.class), any(String.class));
    }

    @Test
    void actualizaEstadoSinRespuestaNiUsuarioYValidaEstado() {
        Incidencia incidencia = new Incidencia();
        when(incidenciaRepository.findById(2L)).thenReturn(Optional.of(incidencia));
        assertThrows(IllegalArgumentException.class,
                () -> service().actualizarEstado(2L, null, "", "admin@example.com"));
        service().actualizarEstado(2L, EstadoIncidencia.EN_REVISION, " ", "admin@example.com");
        assertEquals(null, incidencia.getRespuesta());
        verify(notificacionService, never()).crear(any(), any(), any(), any());
    }

    @Test
    void agregaComentariosDeResidenteYAdminYRechazaAcceso() {
        Incidencia incidencia = incidencia();
        when(incidenciaRepository.findById(1L)).thenReturn(Optional.of(incidencia));
        when(comentarioRepository.save(any(IncidenciaComentario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(usuarioRepository.findByRolAndActivoTrue(Rol.ADMIN)).thenReturn(List.of());

        IncidenciaComentario residente = service().agregarComentario(
                1L, "  Actualización  ", "residente@example.com", " Ana ", false);
        assertEquals("Actualización", residente.getContenido());
        IncidenciaComentario admin = service().agregarComentario(
                1L, "Respuesta", "admin@example.com", null, true);
        assertEquals("admin@example.com", admin.getAutorNombre());
        assertThrows(IllegalArgumentException.class,
                () -> service().agregarComentario(1L, "Comentario", "otro@example.com", "Otro", false));
        assertThrows(IllegalArgumentException.class,
                () -> service().agregarComentario(1L, " ", "admin@example.com", "Admin", true));
    }

    @Test
    void buscaAdjuntoYValidaPropietario() {
        Incidencia incidencia = incidencia();
        IncidenciaAdjunto adjunto = new IncidenciaAdjunto();
        when(incidenciaRepository.findById(1L)).thenReturn(Optional.of(incidencia));
        when(adjuntoRepository.findByIdAndIncidenciaId(5L, 1L)).thenReturn(Optional.of(adjunto));

        assertEquals(adjunto, service().buscarAdjunto(1L, 5L, "residente@example.com", false));
        assertEquals(adjunto, service().buscarAdjunto(1L, 5L, "admin@example.com", true));
        assertThrows(IllegalArgumentException.class,
                () -> service().buscarAdjunto(1L, 5L, "otro@example.com", false));
        when(adjuntoRepository.findByIdAndIncidenciaId(8L, 1L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> service().buscarAdjunto(1L, 8L, "admin@example.com", true));
    }

    @Test
    void agregaAdjuntoSeguroYEliminaArchivoSiFallaPersistencia() {
        Incidencia incidencia = incidencia();
        when(incidenciaRepository.findById(1L)).thenReturn(Optional.of(incidencia));
        when(archivoStorageService.guardar(any())).thenReturn("interno-1");
        when(adjuntoRepository.save(any(IncidenciaAdjunto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "../evidencia.pdf", "application/pdf", "pdf".getBytes());

        IncidenciaAdjunto guardado = service().agregarAdjunto(1L, archivo, "admin@example.com", true);
        assertEquals("evidencia.pdf", guardado.getNombreOriginal());
        assertEquals("interno-1", guardado.getNombreInterno());
        verify(incidenciaRepository).save(incidencia);

        doThrow(new IllegalStateException("error")).when(adjuntoRepository).save(any(IncidenciaAdjunto.class));
        assertThrows(IllegalStateException.class,
                () -> service().agregarAdjunto(1L, archivo, "admin@example.com", true));
        verify(archivoStorageService).eliminar("interno-1");
    }

    @Test
    void noPermiteAdjuntoDeOtroResidente() {
        when(incidenciaRepository.findById(1L)).thenReturn(Optional.of(incidencia()));
        MockMultipartFile archivo = new MockMultipartFile("archivo", "evidencia.pdf", "application/pdf", "x".getBytes());
        assertThrows(IllegalArgumentException.class,
                () -> service().agregarAdjunto(1L, archivo, "otro@example.com", false));
        verify(archivoStorageService, never()).guardar(any());
    }

    private IncidenciaService service() {
        return new IncidenciaService(incidenciaRepository, apartamentoRepository, comentarioRepository,
                adjuntoRepository, usuarioRepository, notificacionService, archivoStorageService);
    }

    private Incidencia incidencia() {
        Incidencia incidencia = new Incidencia();
        incidencia.setId(1L);
        incidencia.setAsunto("Fuga");
        incidencia.setResidente(residente());
        return incidencia;
    }

    private Residente residente() {
        Usuario usuario = new Usuario();
        usuario.setEmail("residente@example.com");
        Residente residente = new Residente();
        residente.setId(4L);
        residente.setUsuario(usuario);
        Apartamento apartamento = new Apartamento();
        apartamento.setId(10L);
        residente.setApartamento(apartamento);
        return residente;
    }
}
