package com.nexur.nexur.service;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.EstadoVisitante;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.model.Visitante;
import com.nexur.nexur.repository.ResidenteRepository;
import com.nexur.nexur.repository.VisitanteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VisitanteServiceTest {

    @Mock
    private VisitanteRepository visitanteRepository;
    @Mock
    private ResidenteRepository residenteRepository;

    @Test
    void residenteCreaSolicitudPendienteConSuApartamento() {
        Apartamento apartamento = new Apartamento();
        apartamento.setId(4L);
        Residente residente = new Residente();
        residente.setApartamento(apartamento);
        when(residenteRepository.findByUsuarioEmail("residente@example.com"))
                .thenReturn(Optional.of(residente));
        when(visitanteRepository.save(any(Visitante.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Visitante visitante = new Visitante();
        visitante.setNombre("  Juan Visitante  ");
        visitante.setDocumento(" 90123456 ");
        visitante.setEstado(EstadoVisitante.DENTRO);

        VisitanteService service = new VisitanteService(visitanteRepository, residenteRepository);
        Visitante guardado = service.solicitar(visitante, "residente@example.com");

        assertEquals("Juan Visitante", guardado.getNombre());
        assertEquals("90123456", guardado.getDocumento());
        assertEquals(apartamento, guardado.getApartamento());
        assertEquals(EstadoVisitante.PENDIENTE, guardado.getEstado());
        assertNull(guardado.getFechaEntrada());
        assertNull(guardado.getFechaSalida());
    }

    @Test
    void noPermiteRegistrarEntradaDeSolicitudPendiente() {
        Visitante visitante = new Visitante();
        visitante.setId(8L);
        visitante.setEstado(EstadoVisitante.PENDIENTE);
        when(visitanteRepository.findById(8L)).thenReturn(Optional.of(visitante));

        VisitanteService service = new VisitanteService(visitanteRepository, residenteRepository);

        assertThrows(IllegalArgumentException.class, () -> service.registrarEntrada(8L));
    }

    @Test
    void noPermiteRegistrarSalidaSiElVisitanteNoEstaDentro() {
        Visitante visitante = new Visitante();
        visitante.setId(9L);
        visitante.setEstado(EstadoVisitante.APROBADA);
        when(visitanteRepository.findById(9L)).thenReturn(Optional.of(visitante));

        VisitanteService service = new VisitanteService(visitanteRepository, residenteRepository);

        assertThrows(IllegalArgumentException.class, () -> service.registrarSalida(9L));
    }

    @Test
    void listaVisitantesPorEstadoYApartamento() {
        when(visitanteRepository.findAll()).thenReturn(List.of());
        when(visitanteRepository.findByEstadoOrderByFechaEntradaDesc(EstadoVisitante.DENTRO)).thenReturn(List.of());
        when(visitanteRepository.findByApartamentoIdAndEstadoOrderByFechaEntradaDesc(4L, EstadoVisitante.DENTRO))
                .thenReturn(List.of());
        when(visitanteRepository.findByEstadoOrderByFechaEntradaDesc(EstadoVisitante.PENDIENTE)).thenReturn(List.of());
        when(visitanteRepository.findByApartamentoId(4L)).thenReturn(List.of());
        VisitanteService service = new VisitanteService(visitanteRepository, residenteRepository);
        assertEquals(0, service.listarVisitantes().size());
        assertEquals(0, service.listarVisitantesActivos().size());
        assertEquals(0, service.listarVisitantesActivosPorApartamento(4L).size());
        assertEquals(0, service.listarSolicitudesPendientes().size());
        assertEquals(0, service.buscarPorApartamento(4L).size());
    }

    @Test
    void validaSolicitudYResidenteSinApartamento() {
        VisitanteService service = new VisitanteService(visitanteRepository, residenteRepository);
        assertThrows(IllegalArgumentException.class, () -> service.solicitar(null, "residente@example.com"));
        Visitante sinDocumento = new Visitante();
        sinDocumento.setNombre("Visitante");
        assertThrows(IllegalArgumentException.class, () -> service.solicitar(sinDocumento, "residente@example.com"));
        Visitante visitante = new Visitante();
        visitante.setNombre("Visitante");
        visitante.setDocumento("12345678");
        when(residenteRepository.findByUsuarioEmail("residente@example.com")).thenReturn(Optional.of(new Residente()));
        assertThrows(IllegalArgumentException.class, () -> service.solicitar(visitante, "residente@example.com"));
        when(residenteRepository.findByUsuarioEmail("sin-cuenta@example.com")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.solicitar(visitante, "sin-cuenta@example.com"));
    }

    @Test
    void porteriaApruebaRechazaRegistraEntradaYSalida() {
        Visitante visitante = new Visitante();
        visitante.setId(10L);
        visitante.setEstado(EstadoVisitante.PENDIENTE);
        when(visitanteRepository.findById(10L)).thenReturn(Optional.of(visitante));
        when(visitanteRepository.save(visitante)).thenReturn(visitante);
        VisitanteService service = new VisitanteService(visitanteRepository, residenteRepository);

        service.aprobarSolicitud(10L);
        assertEquals(EstadoVisitante.APROBADA, visitante.getEstado());
        service.registrarEntrada(10L);
        assertEquals(EstadoVisitante.DENTRO, visitante.getEstado());
        assertNotNull(visitante.getFechaEntrada());
        service.registrarSalida(10L);
        assertEquals(EstadoVisitante.FINALIZADA, visitante.getEstado());
        assertNotNull(visitante.getFechaSalida());
        verify(visitanteRepository, org.mockito.Mockito.times(3)).save(visitante);
    }

    @Test
    void rechazaSolicitudConMotivoLargoYUsaMotivoPorDefecto() {
        Visitante visitante = new Visitante();
        visitante.setId(11L);
        visitante.setEstado(EstadoVisitante.PENDIENTE);
        when(visitanteRepository.findById(11L)).thenReturn(Optional.of(visitante));
        VisitanteService service = new VisitanteService(visitanteRepository, residenteRepository);
        String motivoLargo = "x".repeat(501);
        assertThrows(IllegalArgumentException.class, () -> service.rechazarSolicitud(11L, motivoLargo));
        when(visitanteRepository.save(visitante)).thenReturn(visitante);
        service.rechazarSolicitud(11L, " ");
        assertEquals(EstadoVisitante.RECHAZADA, visitante.getEstado());
        assertEquals("Solicitud rechazada por portería", visitante.getMotivoRechazo());
    }

    @Test
    void buscarVisitanteInexistenteYEstadosIncorrectos() {
        when(visitanteRepository.findById(12L)).thenReturn(Optional.empty());
        VisitanteService service = new VisitanteService(visitanteRepository, residenteRepository);
        assertThrows(IllegalArgumentException.class, () -> service.aprobarSolicitud(12L));
        Visitante visitante = new Visitante();
        visitante.setId(13L);
        visitante.setEstado(EstadoVisitante.APROBADA);
        when(visitanteRepository.findById(13L)).thenReturn(Optional.of(visitante));
        assertThrows(IllegalArgumentException.class, () -> service.aprobarSolicitud(13L));
        assertThrows(IllegalArgumentException.class, () -> service.rechazarSolicitud(13L, "motivo"));
    }
}
