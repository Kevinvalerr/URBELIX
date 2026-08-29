package com.nexur.nexur.service;

import com.nexur.nexur.model.Pago;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.model.enums.EstadoPago;
import com.nexur.nexur.model.enums.MetodoPago;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

/**
 * Sandbox local para probar el ciclo de pago sin contactar a Wompi.
 * El resultado se procesa mediante el mismo servicio de eventos firmado.
 */
@Service
public class PagoSimulacionService {

    private final PagoService pagoService;
    private final WompiService wompiService;
    private final boolean habilitada;
    private final String simulationSecret;

    public PagoSimulacionService(PagoService pagoService,
                                 WompiService wompiService,
                                 @Value("${app.payments.simulation-enabled:false}") boolean habilitada,
                                 @Value("${app.payments.simulation-secret:}") String simulationSecret) {
        this.pagoService = pagoService;
        this.wompiService = wompiService;
        this.habilitada = habilitada;
        this.simulationSecret = simulationSecret;
    }

    public boolean estaHabilitada() {
        return habilitada && StringUtils.hasText(simulationSecret);
    }

    public boolean puedeSimular(Pago pago) {
        return habilitada
                && StringUtils.hasText(simulationSecret)
                && pago != null
                && (pago.getMetodo() == MetodoPago.PSE || pago.getMetodo() == MetodoPago.TARJETA)
                && (pago.getEstadoPago() == EstadoPago.PENDIENTE
                || pago.getEstadoPago() == EstadoPago.VENCIDO)
                && StringUtils.hasText(pago.getReferenciaPago());
    }

    @Transactional
    public Resultado simular(Long pagoId, String email, String estadoSolicitado) {
        if (!habilitada) {
            throw new IllegalStateException("El sandbox local de pagos está desactivado");
        }
        if (!StringUtils.hasText(simulationSecret)) {
            throw new IllegalStateException("El sandbox local no tiene configurado su secreto de eventos");
        }

        Pago pago = pagoService.buscarPorId(pagoId);
        validarPropietario(pago, email);
        if (pago.getMetodo() != MetodoPago.PSE && pago.getMetodo() != MetodoPago.TARJETA) {
            throw new IllegalArgumentException("El sandbox local solo acepta pagos configurados para checkout en línea");
        }
        if (pago.getEstadoPago() != EstadoPago.PENDIENTE
                && pago.getEstadoPago() != EstadoPago.VENCIDO) {
            throw new IllegalArgumentException("Solo se puede simular un pago pendiente o vencido");
        }
        if (!StringUtils.hasText(pago.getReferenciaPago())) {
            throw new IllegalArgumentException("Primero debes iniciar el pago para generar su referencia");
        }

        String estado = normalizarEstado(estadoSolicitado);
        long montoEnCentavos = wompiService.montoEnCentavos(pago.getMonto());
        String transactionId = "sim-" + UUID.randomUUID();
        long timestamp = Instant.now().getEpochSecond();
        String checksum = sha256(transactionId + estado + montoEnCentavos + timestamp + simulationSecret);
        String cuerpo = construirEvento(transactionId, estado, montoEnCentavos,
                pago.getReferenciaPago().trim(), timestamp, checksum);

        WompiService.Resultado resultadoWompi = wompiService.procesarEventoSimulado(
                cuerpo, checksum, simulationSecret);
        return new Resultado(transactionId, estado, resultadoWompi);
    }

    private void validarPropietario(Pago pago, String email) {
        Usuario usuario = pago.getResidente() == null ? null : pago.getResidente().getUsuario();
        if (usuario == null || !StringUtils.hasText(email)
                || !email.trim().equalsIgnoreCase(usuario.getEmail())) {
            throw new IllegalArgumentException("No puede simular este pago");
        }
    }

    private String normalizarEstado(String estadoSolicitado) {
        String estado = estadoSolicitado == null ? "" : estadoSolicitado.trim().toUpperCase(Locale.ROOT);
        return switch (estado) {
            case "APPROVED", "APROBADO" -> "APPROVED";
            case "PENDING", "PENDIENTE" -> "PENDING";
            case "DECLINED", "RECHAZADO", "RECHAZADA" -> "DECLINED";
            case "VOIDED", "ANULADO", "ANULADA" -> "VOIDED";
            case "ERROR" -> "ERROR";
            default -> throw new IllegalArgumentException("Resultado de simulación no válido");
        };
    }

    private String construirEvento(String transactionId, String estado, long monto,
                                   String referencia, long timestamp, String checksum) {
        return "{\"event\":\"transaction.updated\","
                + "\"data\":{\"transaction\":{"
                + "\"id\":\"" + escaparJson(transactionId) + "\","
                + "\"status\":\"" + estado + "\","
                + "\"amount_in_cents\":" + monto + ","
                + "\"reference\":\"" + escaparJson(referencia) + "\"}},"
                + "\"signature\":{\"properties\":[\"transaction.id\","
                + "\"transaction.status\",\"transaction.amount_in_cents\"],"
                + "\"checksum\":\"" + checksum + "\"},"
                + "\"timestamp\":" + timestamp + "}";
    }

    private String escaparJson(String valor) {
        return valor.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n");
    }

    private String sha256(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo firmar la transacción simulada", exception);
        }
    }

    public record Resultado(String transactionId, String estadoProveedor,
                            WompiService.Resultado resultadoWompi) {
    }
}
