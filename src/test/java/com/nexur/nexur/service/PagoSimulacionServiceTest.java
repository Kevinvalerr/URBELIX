package com.nexur.nexur.service;

import com.nexur.nexur.model.Pago;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.model.enums.EstadoPago;
import com.nexur.nexur.model.enums.MetodoPago;
import com.nexur.nexur.repository.PagoRepository;
import com.nexur.nexur.repository.PagoWebhookEventoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoSimulacionServiceTest {

    @Mock
    private PagoService pagoService;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private PagoWebhookEventoRepository eventoRepository;

    @Test
    void aprobacionSimuladaPasaPorElProcesadorRealDeEventos() {
        Pago pago = pagoPendiente();
        when(pagoService.buscarPorId(8L)).thenReturn(pago);
        when(pagoRepository.findByReferenciaPago("PSE-TEST")).thenReturn(Optional.of(pago));
        when(eventoRepository.existsByEventoId(anyString())).thenReturn(false);
        when(pagoRepository.save(any(Pago.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PagoSimulacionService service = new PagoSimulacionService(
                pagoService, wompiService("provider-secret"), true, "sandbox-secret");

        PagoSimulacionService.Resultado resultado = service.simular(
                8L, "residente@example.com", "APPROVED");

        assertEquals("APPROVED", resultado.estadoProveedor());
        assertEquals(WompiService.Resultado.PROCESADO, resultado.resultadoWompi());
        assertTrue(resultado.transactionId().startsWith("sim-"));
        assertEquals(EstadoPago.PAGADO, pago.getEstadoPago());
        assertNotNull(pago.getFechaPago());
        verify(pagoRepository).save(pago);
        verify(eventoRepository).save(any());
    }

    @Test
    void resultadoRechazadoConservaElPagoPendiente() {
        Pago pago = pagoPendiente();
        when(pagoService.buscarPorId(8L)).thenReturn(pago);
        when(pagoRepository.findByReferenciaPago("PSE-TEST")).thenReturn(Optional.of(pago));
        when(eventoRepository.existsByEventoId(anyString())).thenReturn(false);

        PagoSimulacionService service = new PagoSimulacionService(
                pagoService, wompiService("provider-secret"), true, "sandbox-secret");

        PagoSimulacionService.Resultado resultado = service.simular(
                8L, "residente@example.com", "DECLINED");

        assertEquals("DECLINED", resultado.estadoProveedor());
        assertEquals(EstadoPago.PENDIENTE, pago.getEstadoPago());
        verify(pagoRepository, never()).save(any(Pago.class));
        verify(eventoRepository).save(any());
    }

    @Test
    void resultadoAnuladoConservaElPagoPendiente() {
        Pago pago = pagoPendiente();
        when(pagoService.buscarPorId(8L)).thenReturn(pago);
        when(pagoRepository.findByReferenciaPago("PSE-TEST")).thenReturn(Optional.of(pago));
        when(eventoRepository.existsByEventoId(anyString())).thenReturn(false);

        PagoSimulacionService service = new PagoSimulacionService(
                pagoService, wompiService("provider-secret"), true, "sandbox-secret");

        PagoSimulacionService.Resultado resultado = service.simular(
                8L, "residente@example.com", "VOIDED");

        assertEquals("VOIDED", resultado.estadoProveedor());
        assertEquals(EstadoPago.PENDIENTE, pago.getEstadoPago());
        verify(pagoRepository, never()).save(any(Pago.class));
        verify(eventoRepository).save(any());
    }

    @Test
    void noPermiteSimularUnPagoDeOtroResidente() {
        Pago pago = pagoPendiente();
        when(pagoService.buscarPorId(8L)).thenReturn(pago);

        PagoSimulacionService service = new PagoSimulacionService(
                pagoService, wompiService("provider-secret"), true, "sandbox-secret");

        assertThrows(IllegalArgumentException.class,
                () -> service.simular(8L, "otro@example.com", "APPROVED"));
        verify(pagoRepository, never()).findByReferenciaPago(anyString());
    }

    @Test
    void noPermiteResultadoDesconocido() {
        Pago pago = pagoPendiente();
        when(pagoService.buscarPorId(8L)).thenReturn(pago);

        PagoSimulacionService service = new PagoSimulacionService(
                pagoService, wompiService("provider-secret"), true, "sandbox-secret");

        assertThrows(IllegalArgumentException.class,
                () -> service.simular(8L, "residente@example.com", "CUALQUIERA"));
    }

    private Pago pagoPendiente() {
        Usuario usuario = new Usuario();
        usuario.setEmail("residente@example.com");

        Residente residente = new Residente();
        residente.setId(4L);
        residente.setUsuario(usuario);

        Pago pago = new Pago();
        pago.setMonto(new BigDecimal("100000"));
        pago.setEstadoPago(EstadoPago.PENDIENTE);
        pago.setMetodo(MetodoPago.PSE);
        pago.setReferenciaPago("PSE-TEST");
        pago.setResidente(residente);
        return pago;
    }

    private WompiService wompiService(String eventsSecret) {
        return new WompiService(pagoRepository, eventoRepository, JsonMapper.builder().build(),
                "pub_test_demo", "integrity_demo", eventsSecret,
                "http://localhost:8080/v1/", "http://localhost:8080");
    }
}
