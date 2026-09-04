package com.nexur.nexur.service;

import com.nexur.nexur.model.Pago;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.model.enums.EstadoPago;
import com.nexur.nexur.model.enums.MetodoPago;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.UUID;

/**
 * Sandbox local para probar el ciclo de pago sin contactar proveedores externos.
 * Solo cambia el estado del pago cuando el escenario seleccionado es aprobado.
 */
@Service
public class PagoSimulacionService {

    private final PagoService pagoService;
    private final boolean habilitada;

    public PagoSimulacionService(PagoService pagoService,
                                 @Value("${app.payments.simulation-enabled:true}") boolean habilitada) {
        this.pagoService = pagoService;
        this.habilitada = habilitada;
    }

    public boolean estaHabilitada() {
        return habilitada;
    }

    public boolean puedeSimular(Pago pago) {
        return habilitada
                && pago != null
                && esMetodoDePagoEnLinea(pago.getMetodo())
                && (pago.getEstadoPago() == EstadoPago.PENDIENTE
                || pago.getEstadoPago() == EstadoPago.VENCIDO)
                && StringUtils.hasText(pago.getReferenciaPago());
    }

    @Transactional
    public Resultado simular(Long pagoId, String email, String estadoSolicitado) {
        if (!habilitada) {
            throw new IllegalStateException("El sandbox local de pagos está desactivado");
        }
        Pago pago = pagoService.buscarPorId(pagoId);
        validarPropietario(pago, email);
        if (!esMetodoDePagoEnLinea(pago.getMetodo())) {
            throw new IllegalArgumentException(
                    "El sandbox local solo acepta pagos PSE o tarjeta");
        }
        if (pago.getEstadoPago() != EstadoPago.PENDIENTE
                && pago.getEstadoPago() != EstadoPago.VENCIDO) {
            throw new IllegalArgumentException("Solo se puede simular un pago pendiente o vencido");
        }
        if (!StringUtils.hasText(pago.getReferenciaPago())) {
            throw new IllegalArgumentException("Primero debes iniciar el pago para generar su referencia");
        }

        String estado = normalizarEstado(estadoSolicitado);
        String transactionId = "SIM-" + UUID.randomUUID();
        pagoService.registrarResultadoSimulado(pago, estado, transactionId);
        return new Resultado(transactionId, estado);
    }

    private boolean esMetodoDePagoEnLinea(MetodoPago metodo) {
        return metodo == MetodoPago.PSE || metodo == MetodoPago.TARJETA;
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

    public record Resultado(String transactionId, String estadoProveedor) {
    }
}
