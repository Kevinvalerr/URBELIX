package com.nexur.nexur.service;

import com.nexur.nexur.model.ReporteRegistro;
import com.nexur.nexur.model.Pago;
import com.nexur.nexur.model.Reserva;
import com.nexur.nexur.model.Visitante;
import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.model.enums.TipoEspacio;
import com.nexur.nexur.repository.PagoRepository;
import com.nexur.nexur.repository.ReservaRepository;
import com.nexur.nexur.repository.VisitanteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private VisitanteRepository visitanteRepository;

    @Test
    void rechazaRangoDeFechasInvertido() {
        ReporteService service = new ReporteService(pagoRepository, reservaRepository, visitanteRepository);

        assertThrows(IllegalArgumentException.class, () -> service.filtrarRegistros(
                "TODOS", LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 1)));
    }

    @Test
    void filtraPagosDentroDelTipoSolicitado() {
        when(pagoRepository.findByFechaBetween(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)))
                .thenReturn(List.of());
        ReporteService service = new ReporteService(pagoRepository, reservaRepository, visitanteRepository);

        List<ReporteRegistro> registros = service.filtrarRegistros(
                "PAGOS", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        assertEquals(0, registros.size());
    }

    @Test
    void filtraCadaTipoYUsaFechasPorDefecto() {
        when(pagoRepository.findByFechaBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(pago()));
        when(reservaRepository.findByFechaInicioBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(reserva()));
        when(visitanteRepository.findByFechaEntradaBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(visitante()));
        ReporteService service = new ReporteService(pagoRepository, reservaRepository, visitanteRepository);

        assertEquals(3, service.filtrarRegistros(null, null, null).size());
        assertEquals(1, service.filtrarRegistros("reservas", LocalDate.now().minusDays(1), LocalDate.now()).size());
        assertEquals(1, service.filtrarRegistros("VISITANTES", LocalDate.now().minusDays(1), LocalDate.now()).size());
    }

    @Test
    void rechazaTipoDesconocidoYMapeaRelacionesNulas() {
        ReporteService service = new ReporteService(pagoRepository, reservaRepository, visitanteRepository);
        assertThrows(IllegalArgumentException.class, () -> service.filtrarRegistros("OTRO",
                LocalDate.now().minusDays(1), LocalDate.now()));

        Pago pago = pago();
        pago.setResidente(null);
        pago.setApartamento(null);
        pago.setFecha(null);
        pago.setCreadoEn(LocalDateTime.now());
        when(pagoRepository.findByFechaBetween(any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of(pago));
        assertEquals(1, service.filtrarRegistros("PAGOS", LocalDate.now().minusDays(1), LocalDate.now()).size());
    }

    private Pago pago() {
        Pago pago = new Pago();
        pago.setMonto(java.math.BigDecimal.ONE);
        Residente residente = new Residente();
        residente.setNombre("Ana");
        Apartamento apartamento = new Apartamento();
        apartamento.setNumero("101");
        pago.setResidente(residente);
        pago.setApartamento(apartamento);
        pago.setFecha(LocalDate.now());
        return pago;
    }

    private Reserva reserva() {
        Reserva reserva = new Reserva();
        reserva.setTipoEspacio(TipoEspacio.BBQ);
        reserva.setCreadoEn(LocalDateTime.now());
        return reserva;
    }

    private Visitante visitante() {
        Visitante visitante = new Visitante();
        visitante.setNombre("Visitante");
        visitante.setFechaEntrada(LocalDateTime.now());
        return visitante;
    }
}
