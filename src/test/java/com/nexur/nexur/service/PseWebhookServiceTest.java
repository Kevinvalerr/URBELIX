package com.nexur.nexur.service;

import com.nexur.nexur.model.Pago;
import com.nexur.nexur.model.enums.EstadoPago;
import com.nexur.nexur.repository.PagoRepository;
import com.nexur.nexur.repository.PagoWebhookEventoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PseWebhookServiceTest {

    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private PagoWebhookEventoRepository eventoRepository;
    @Mock
    private NotificacionService notificacionService;

    @Test
    void validaFirmaHmacDelProveedor() throws Exception {
        PseWebhookService service = service();
        String cuerpo = "{\"eventoId\":\"evt-1\"}";
        String firma = hmac(cuerpo, "secreto-prueba");

        assertTrue(service.firmaValida(cuerpo, "sha256=" + firma));
        assertFalse(service.firmaValida(cuerpo + "-alterado", firma));
    }

    @Test
    void procesaAprobacionYNoProcesaElMismoEventoDosVeces() {
        Pago pago = pagoPendiente();
        when(eventoRepository.existsByEventoId("evt-1")).thenReturn(false, true);
        when(pagoRepository.findByReferenciaPago("PSE-1")).thenReturn(Optional.of(pago));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PseWebhookService service = service();
        PseWebhookService.PseWebhookRequest solicitud = new PseWebhookService.PseWebhookRequest(
                "evt-1", "PSE-1", "APPROVED", new BigDecimal("100000"));

        assertEquals(PseWebhookService.Resultado.PROCESADO, service.procesar(solicitud));
        assertEquals(EstadoPago.PAGADO, pago.getEstadoPago());
        assertNotNull(pago.getFechaPago());
        assertEquals(PseWebhookService.Resultado.DUPLICADO, service.procesar(solicitud));
        verify(eventoRepository).save(any());
        verify(pagoRepository, times(1)).findByReferenciaPago("PSE-1");
    }

    @Test
    void rechazaMontoDiferenteAlPago() {
        Pago pago = pagoPendiente();
        when(eventoRepository.existsByEventoId("evt-2")).thenReturn(false);
        when(pagoRepository.findByReferenciaPago("PSE-1")).thenReturn(Optional.of(pago));

        PseWebhookService service = service();

        assertThrows(IllegalArgumentException.class, () -> service.procesar(
                new PseWebhookService.PseWebhookRequest(
                        "evt-2", "PSE-1", "APPROVED", new BigDecimal("99999"))));
        verify(eventoRepository, never()).save(any());
    }

    private PseWebhookService service() {
        return new PseWebhookService(pagoRepository, eventoRepository, notificacionService,
                "secreto-prueba");
    }

    private Pago pagoPendiente() {
        Pago pago = new Pago();
        pago.setMonto(new BigDecimal("100000"));
        pago.setEstadoPago(EstadoPago.PENDIENTE);
        pago.setReferenciaPago("PSE-1");
        return pago;
    }

    private String hmac(String cuerpo, String secreto) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(cuerpo.getBytes(StandardCharsets.UTF_8)));
    }
}
