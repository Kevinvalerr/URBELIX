package com.nexur.nexur.controller;

import com.nexur.nexur.service.ApartamentoService;
import com.nexur.nexur.service.PagoService;
import com.nexur.nexur.service.ReporteService;
import com.nexur.nexur.service.ReportesFastApiService;
import com.nexur.nexur.service.ReservaService;
import com.nexur.nexur.service.ResidenteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteControllerTest {

    @Mock
    private ReporteService reporteService;
    @Mock
    private ResidenteService residenteService;
    @Mock
    private ApartamentoService apartamentoService;
    @Mock
    private PagoService pagoService;
    @Mock
    private ReservaService reservaService;
    @Mock
    private ReportesFastApiService reportesFastApiService;

    @Test
    void usaGeneradorLocalCuandoFastApiNoEstaDisponible() {
        when(reporteService.filtrarRegistros("TODOS", null, null)).thenReturn(List.of());
        when(reportesFastApiService.generarPdf(List.of()))
                .thenThrow(new ResourceAccessException("FastAPI apagado"));

        ReporteController controller = new ReporteController(
                reporteService, residenteService, apartamentoService, pagoService,
                reservaService, reportesFastApiService, true);

        ResponseEntity<byte[]> respuesta = controller.generarPdf("TODOS", null, null);

        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals(MediaType.APPLICATION_PDF, respuesta.getHeaders().getContentType());
        assertNotNull(respuesta.getBody());
        assertTrue(respuesta.getBody().length > 0);
    }
}
