package com.nexur.nexur.service;

import com.nexur.nexur.model.Pago;
import com.nexur.nexur.model.PagoWebhookEvento;
import com.nexur.nexur.model.enums.EstadoPago;
import com.nexur.nexur.repository.PagoRepository;
import com.nexur.nexur.repository.PagoWebhookEventoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class WompiService {

    private static final String CURRENCY = "COP";
    private static final Logger LOGGER = LoggerFactory.getLogger(WompiService.class);

    private final PagoRepository pagoRepository;
    private final PagoWebhookEventoRepository eventoRepository;
    private final JsonMapper jsonMapper;
    private final String publicKey;
    private final String integritySecret;
    private final String eventsSecret;
    private final String wompiBaseUrl;
    private final String applicationBaseUrl;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public WompiService(PagoRepository pagoRepository,
                        PagoWebhookEventoRepository eventoRepository,
                        JsonMapper jsonMapper,
                        @Value("${wompi.public-key:}") String publicKey,
                        @Value("${wompi.integrity-secret:}") String integritySecret,
                        @Value("${wompi.events-secret:}") String eventsSecret,
                        @Value("${wompi.base-url:https://sandbox.wompi.co/v1/}") String wompiBaseUrl,
                        @Value("${app.base-url:http://localhost:8080}") String applicationBaseUrl) {
        this.pagoRepository = pagoRepository;
        this.eventoRepository = eventoRepository;
        this.jsonMapper = jsonMapper;
        this.publicKey = publicKey;
        this.integritySecret = integritySecret;
        this.eventsSecret = eventsSecret;
        this.wompiBaseUrl = wompiBaseUrl;
        this.applicationBaseUrl = applicationBaseUrl;
    }

    public boolean estaConfigurado() {
        return StringUtils.hasText(publicKey) && StringUtils.hasText(integritySecret);
    }

    public String getPublicKey() {
        return publicKey;
    }

    public long montoEnCentavos(BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del pago no es válido");
        }
        return monto.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
    }

    public String firmaIntegridad(String referencia, BigDecimal monto) {
        if (!StringUtils.hasText(referencia) || !StringUtils.hasText(integritySecret)) {
            throw new IllegalArgumentException("Wompi no está configurado para pagos");
        }
        String material = referencia.trim() + montoEnCentavos(monto) + CURRENCY + integritySecret;
        return sha256(material);
    }

    public String urlRedireccion(Long pagoId) {
        String base = applicationBaseUrl == null ? "" : applicationBaseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/pagos/" + pagoId;
    }

    /**
     * Consulta el estado real que Wompi devuelve al navegador despues del checkout.
     * La respuesta se valida contra el pago local antes de cambiar su estado.
     */
    @Transactional
    public SincronizacionResultado sincronizarTransaccion(Pago pago, String transactionId) {
        if (pago == null || !StringUtils.hasText(transactionId) || !estaConfigurado()) {
            return SincronizacionResultado.NO_DISPONIBLE;
        }
        String id = transactionId.trim();
        if (!id.matches("[A-Za-z0-9._:-]{1,120}")) {
            return SincronizacionResultado.NO_VALIDA;
        }

        JsonNode transaccion = consultarTransaccion(id);
        if (transaccion == null) {
            return SincronizacionResultado.NO_DISPONIBLE;
        }

        String idRemoto = texto(transaccion.path("id"));
        String referenciaRemota = texto(transaccion.path("reference"));
        JsonNode montoRemoto = transaccion.path("amount_in_cents");
        String estadoRemoto = texto(transaccion.path("status"));
        if (!id.equals(idRemoto) || !StringUtils.hasText(referenciaRemota)
                || !montoRemoto.isIntegralNumber() || !StringUtils.hasText(estadoRemoto)) {
            return SincronizacionResultado.NO_VALIDA;
        }

        long montoEsperado;
        try {
            montoEsperado = montoEnCentavos(pago.getMonto());
        } catch (IllegalArgumentException exception) {
            return SincronizacionResultado.NO_VALIDA;
        }
        if (!referenciaRemota.trim().equals(pago.getReferenciaPago())
                || montoRemoto.asLong() != montoEsperado) {
            return SincronizacionResultado.NO_VALIDA;
        }

        String estadoNormalizado = estadoRemoto.trim().toUpperCase(Locale.ROOT);
        if ("APPROVED".equals(estadoNormalizado)) {
            if (pago.getEstadoPago() != EstadoPago.PAGADO || pago.getFechaPago() == null) {
                pago.setEstadoPago(EstadoPago.PAGADO);
                pago.setFechaPago(LocalDate.now());
                pagoRepository.save(pago);
            }
            return SincronizacionResultado.CONFIRMADO;
        }
        if ("PENDING".equals(estadoNormalizado)) {
            return SincronizacionResultado.PENDIENTE;
        }
        return SincronizacionResultado.RECHAZADO;
    }

    public boolean firmaEventoValida(String cuerpo, String checksum) {
        if (!StringUtils.hasText(cuerpo) || !StringUtils.hasText(eventsSecret)) {
            return false;
        }
        try {
            JsonNode evento = jsonMapper.readTree(cuerpo);
            return firmaEventoValida(evento, checksum, eventsSecret);
        } catch (JacksonException exception) {
            return false;
        }
    }

    @Transactional
    public Resultado procesarEvento(String cuerpo, String checksum) {
        return procesarEvento(cuerpo, checksum, eventsSecret);
    }

    /**
     * Procesa un evento de sandbox con un secreto aislado del proveedor real.
     * Comparte exactamente las mismas validaciones de estructura, firma, monto,
     * referencia, estado e idempotencia del webhook externo.
     */
    @Transactional
    public Resultado procesarEventoSimulado(String cuerpo, String checksum, String simulationSecret) {
        if (!StringUtils.hasText(simulationSecret)) {
            throw new IllegalArgumentException("El sandbox local no tiene configurado su secreto de eventos");
        }
        return procesarEvento(cuerpo, checksum, simulationSecret);
    }

    private Resultado procesarEvento(String cuerpo, String checksum, String secret) {
        JsonNode evento;
        try {
            evento = jsonMapper.readTree(cuerpo);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("El evento Wompi no contiene JSON válido");
        }
        if (!firmaEventoValida(evento, checksum, secret)) {
            throw new FirmaInvalidaException();
        }
        if (!"transaction.updated".equals(texto(evento.path("event")))) {
            throw new IllegalArgumentException("Tipo de evento Wompi no soportado");
        }

        JsonNode transaccion = evento.path("data").path("transaction");
        String transactionId = texto(transaccion.path("id"));
        String referencia = texto(transaccion.path("reference"));
        String estado = texto(transaccion.path("status"));
        JsonNode monto = transaccion.path("amount_in_cents");
        String timestamp = texto(evento.path("timestamp"));
        if (!StringUtils.hasText(transactionId) || !StringUtils.hasText(referencia)
                || !StringUtils.hasText(estado) || !monto.isNumber() || !StringUtils.hasText(timestamp)) {
            throw new IllegalArgumentException("El evento Wompi no tiene datos de transacción completos");
        }

        String eventoId = "wompi:" + transactionId + ":" + timestamp;
        if (eventoRepository.existsByEventoId(eventoId)) {
            return Resultado.DUPLICADO;
        }

        Pago pago = pagoRepository.findByReferenciaPago(referencia.trim())
                .orElseThrow(() -> new IllegalArgumentException("Referencia de pago no encontrada"));
        long montoEsperado = montoEnCentavos(pago.getMonto());
        if (monto.asLong() != montoEsperado) {
            throw new IllegalArgumentException("El monto del evento Wompi no coincide con el pago");
        }

        String estadoNormalizado = estado.trim().toUpperCase(Locale.ROOT);
        if ("APPROVED".equals(estadoNormalizado)) {
            if (pago.getEstadoPago() != EstadoPago.PAGADO || pago.getFechaPago() == null) {
                pago.setEstadoPago(EstadoPago.PAGADO);
                pago.setFechaPago(LocalDate.now());
                pagoRepository.save(pago);
            }
        } else if (!"PENDING".equals(estadoNormalizado)
                && !"DECLINED".equals(estadoNormalizado)
                && !"VOIDED".equals(estadoNormalizado)
                && !"ERROR".equals(estadoNormalizado)) {
            throw new IllegalArgumentException("Estado Wompi no soportado");
        }

        PagoWebhookEvento registrado = new PagoWebhookEvento();
        registrado.setEventoId(eventoId);
        registrado.setReferenciaPago(referencia.trim());
        registrado.setEstado(estadoNormalizado);
        registrado.setRecibidoEn(LocalDateTime.now());
        eventoRepository.save(registrado);
        return Resultado.PROCESADO;
    }

    private boolean firmaEventoValida(JsonNode evento, String checksum, String secret) {
        if (evento == null || evento.isMissingNode() || !StringUtils.hasText(secret)) {
            return false;
        }
        String checksumRecibido = StringUtils.hasText(checksum)
                ? checksum
                : texto(evento.path("signature").path("checksum"));
        if (!StringUtils.hasText(checksumRecibido)) {
            return false;
        }
        JsonNode propiedades = evento.path("signature").path("properties");
        String timestamp = texto(evento.path("timestamp"));
        if (!propiedades.isArray() || propiedades.size() == 0 || !StringUtils.hasText(timestamp)) {
            return false;
        }

        StringBuilder material = new StringBuilder();
        for (JsonNode propiedad : propiedades) {
            String ruta = texto(propiedad);
            String valor = valorDeRuta(evento.path("data"), ruta);
            if (!StringUtils.hasText(ruta) || valor == null) {
                return false;
            }
            material.append(valor);
        }
        material.append(timestamp).append(secret);
        String esperado = sha256(material.toString());
        return MessageDigest.isEqual(esperado.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII),
                checksumRecibido.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
    }

    private String valorDeRuta(JsonNode raiz, String ruta) {
        if (!StringUtils.hasText(ruta)) {
            return null;
        }
        JsonNode actual = raiz;
        for (String segmento : ruta.split("\\.")) {
            actual = actual.path(segmento);
        }
        return actual.isMissingNode() || actual.isNull() ? null : actual.asText();
    }

    private JsonNode consultarTransaccion(String transactionId) {
        if (!StringUtils.hasText(wompiBaseUrl) || !StringUtils.hasText(publicKey)) {
            return null;
        }
        String base = wompiBaseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(base + "/transactions/" + transactionId))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + publicKey.trim())
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOGGER.warn("Wompi devolvio HTTP {} al consultar la transaccion {}", response.statusCode(), transactionId);
                return null;
            }
            return jsonMapper.readTree(response.body()).path("data");
        } catch (IOException exception) {
            LOGGER.warn("No se pudo consultar la transaccion Wompi {}", transactionId);
            return null;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.warn("La consulta de la transaccion Wompi {} fue interrumpida", transactionId);
            return null;
        } catch (RuntimeException exception) {
            LOGGER.warn("La respuesta de la transaccion Wompi {} no es valida", transactionId);
            return null;
        }
    }

    private String texto(JsonNode nodo) {
        return nodo == null || nodo.isMissingNode() || nodo.isNull() ? null : nodo.asText();
    }

    private String sha256(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo generar la firma de Wompi", exception);
        }
    }

    public enum Resultado {
        PROCESADO,
        DUPLICADO
    }

    public enum SincronizacionResultado {
        CONFIRMADO,
        PENDIENTE,
        RECHAZADO,
        NO_VALIDA,
        NO_DISPONIBLE
    }

    public static class FirmaInvalidaException extends RuntimeException {
    }
}
