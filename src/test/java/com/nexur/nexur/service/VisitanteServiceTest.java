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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
}
