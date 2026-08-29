package com.nexur.nexur.service;

import com.nexur.nexur.model.ReporteRegistro;
import com.nexur.nexur.repository.PagoRepository;
import com.nexur.nexur.repository.ReservaRepository;
import com.nexur.nexur.repository.VisitanteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

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
}
