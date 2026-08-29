package com.nexur.nexur.service;

import com.sun.net.httpserver.HttpServer;
import com.nexur.nexur.model.Pago;
import com.nexur.nexur.model.PagoWebhookEvento;
import com.nexur.nexur.model.enums.EstadoPago;
import com.nexur.nexur.repository.PagoRepository;
import com.nexur.nexur.repository.PagoWebhookEventoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WompiServiceTest {

    private static final String CHECKSUM_EVENTO_APROBADO =
            "5a18ec5e8fdb7df463e9f94774cba8f583ba21bd04a09ceff2ea68a4bc0aefbe";

    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private PagoWebhookEventoRepository eventoRepository;

    @Test
    void generaFirmaDeIntegridadConElFormatoDeWompi() {
        WompiService service = service(
                "pub_test_demo",
                "prod_integrity_Z5mMke9x0k8gpErbDqwrJXMqsI6SFli6",
                "prod_events_demo");

        String firma = service.firmaIntegridad("sk8-438k4-xmxm392-sn2m", new BigDecimal("24900"));

        assertEquals("37c8407747e595535433ef8f6a811d853cd943046624a0ec04662b17bbf33bf5", firma);
    }

    @Test
    void procesaEventoAprobadoValidandoChecksumMontoYReferencia() {
        Pago pago = new Pago();
        pago.setMonto(new BigDecimal("44900"));
        pago.setEstadoPago(EstadoPago.PENDIENTE);
        when(pagoRepository.findByReferenciaPago("MZQ3X2DE2SMX")).thenReturn(Optional.of(pago));
        when(eventoRepository.existsByEventoId("wompi:1234-1610641025-49201:1530291411"))
                .thenReturn(false);

        WompiService service = service(
                "pub_test_demo",
                "integrity_demo",
                "prod_events_OcHnIzeBl5socpwByQ4hA52Em3USQ93Z");

        String cuerpo = """
                {
                  "event":"transaction.updated",
                  "data":{"transaction":{"id":"1234-1610641025-49201","status":"APPROVED","amount_in_cents":4490000,"reference":"MZQ3X2DE2SMX"}},
                  "signature":{"properties":["transaction.id","transaction.status","transaction.amount_in_cents"],"checksum":"5a18ec5e8fdb7df463e9f94774cba8f583ba21bd04a09ceff2ea68a4bc0aefbe"},
                  "timestamp":1530291411
                }
                """;

        WompiService.Resultado resultado = service.procesarEvento(cuerpo, CHECKSUM_EVENTO_APROBADO);

        assertEquals(WompiService.Resultado.PROCESADO, resultado);
        assertEquals(EstadoPago.PAGADO, pago.getEstadoPago());
        assertNotNull(pago.getFechaPago());
        verify(pagoRepository).save(pago);
        verify(eventoRepository).save(any(PagoWebhookEvento.class));
    }

    @Test
    void rechazaChecksumInvalidoAntesDeConsultarElPago() {
        WompiService service = service("pub_test_demo", "integrity_demo", "events_demo");

        assertThrows(WompiService.FirmaInvalidaException.class,
                () -> service.procesarEvento("{}", "checksum-invalido"));

        verify(pagoRepository, never()).findByReferenciaPago(any());
    }

    @Test
    void procesaElMismoEventoSoloUnaVez() {
        when(eventoRepository.existsByEventoId("wompi:1234-1610641025-49201:1530291411"))
                .thenReturn(true);
        WompiService service = service(
                "pub_test_demo",
                "integrity_demo",
                "prod_events_OcHnIzeBl5socpwByQ4hA52Em3USQ93Z");
        String cuerpo = """
                {
                  "event":"transaction.updated",
                  "data":{"transaction":{"id":"1234-1610641025-49201","status":"APPROVED","amount_in_cents":4490000,"reference":"MZQ3X2DE2SMX"}},
                  "signature":{"properties":["transaction.id","transaction.status","transaction.amount_in_cents"],"checksum":"5a18ec5e8fdb7df463e9f94774cba8f583ba21bd04a09ceff2ea68a4bc0aefbe"},
                  "timestamp":1530291411
                }
                """;

        assertTrue(service.firmaEventoValida(cuerpo, null));
        assertEquals(WompiService.Resultado.DUPLICADO, service.procesarEvento(cuerpo, null));
        verify(pagoRepository, never()).findByReferenciaPago(any());
    }

    @Test
    void sincronizaPagoAprobadoConsultandoLaApiYValidaSusDatos() throws Exception {
        Pago pago = new Pago();
        pago.setMonto(new BigDecimal("1000"));
        pago.setReferenciaPago("PSE-API");
        pago.setEstadoPago(EstadoPago.PENDIENTE);

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/transactions/tx-1", exchange -> {
            byte[] respuesta = "{\"data\":{\"id\":\"tx-1\",\"reference\":\"PSE-API\",\"status\":\"APPROVED\",\"amount_in_cents\":100000}}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, respuesta.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(respuesta);
            }
        });
        server.start();
        try {
            WompiService service = service(
                    "pub_test_demo",
                    "integrity_demo",
                    "events_demo",
                    "http://localhost:" + server.getAddress().getPort() + "/v1/");

            assertEquals(WompiService.SincronizacionResultado.CONFIRMADO,
                    service.sincronizarTransaccion(pago, "tx-1"));
            assertEquals(EstadoPago.PAGADO, pago.getEstadoPago());
            verify(pagoRepository).save(pago);
        } finally {
            server.stop(0);
        }
    }

    private WompiService service(String publicKey, String integritySecret, String eventsSecret) {
        return service(publicKey, integritySecret, eventsSecret, "http://localhost:8080/v1/");
    }

    private WompiService service(String publicKey, String integritySecret, String eventsSecret, String wompiBaseUrl) {
        return new WompiService(pagoRepository, eventoRepository, JsonMapper.builder().build(),
                publicKey, integritySecret, eventsSecret, wompiBaseUrl, "http://localhost:8080");
    }
}
