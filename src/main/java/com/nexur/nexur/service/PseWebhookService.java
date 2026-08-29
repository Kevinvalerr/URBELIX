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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class PseWebhookService {

    private final PagoRepository pagoRepository;
    private final PagoWebhookEventoRepository eventoRepository;
    private final NotificacionService notificacionService;
    private final String secreto;

    public PseWebhookService(PagoRepository pagoRepository,
                             PagoWebhookEventoRepository eventoRepository,
                             NotificacionService notificacionService,
                             @Value("${app.pse.webhook-secret:}") String secreto) {
        this.pagoRepository = pagoRepository;
        this.eventoRepository = eventoRepository;
        this.notificacionService = notificacionService;
        this.secreto = secreto;
    }

    public boolean firmaValida(String cuerpo, String firma) {
        if (!StringUtils.hasText(secreto) || !StringUtils.hasText(cuerpo) || !StringUtils.hasText(firma)) {
            return false;
        }
        String firmaNormalizada = firma.trim();
        if (firmaNormalizada.startsWith("sha256=")) {
            firmaNormalizada = firmaNormalizada.substring("sha256=".length());
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String esperada = HexFormat.of().formatHex(mac.doFinal(cuerpo.getBytes(StandardCharsets.UTF_8)));
            return MessageDigest.isEqual(esperada.getBytes(StandardCharsets.US_ASCII),
                    firmaNormalizada.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
        } catch (Exception exception) {
            return false;
        }
    }

    @Transactional
    public Resultado procesar(PseWebhookRequest solicitud) {
        validarSolicitud(solicitud);
        if (eventoRepository.existsByEventoId(solicitud.eventoId())) {
            return Resultado.DUPLICADO;
        }

        Pago pago = pagoRepository.findByReferenciaPago(solicitud.referenciaPago())
                .orElseThrow(() -> new IllegalArgumentException("Referencia de pago no encontrada"));
        if (pago.getMonto() == null || pago.getMonto().compareTo(solicitud.monto()) != 0) {
            throw new IllegalArgumentException("El monto del webhook no coincide con el pago");
        }

        String estado = solicitud.estado().trim().toUpperCase(Locale.ROOT);
        if (estado.equals("APPROVED") || estado.equals("APROBADO") || estado.equals("PAGADO")) {
            if (pago.getEstadoPago() != EstadoPago.PAGADO || pago.getFechaPago() == null) {
                pago.setEstadoPago(EstadoPago.PAGADO);
                pago.setFechaPago(LocalDate.now());
                pagoRepository.save(pago);
            }
            if (pago.getResidente() != null && pago.getResidente().getUsuario() != null) {
                notificacionService.crear(pago.getResidente().getUsuario(), "Pago PSE aprobado",
                        "El pago con referencia " + pago.getReferenciaPago() + " fue aprobado.", "/pagos");
            }
            estado = "APPROVED";
        } else if (estado.equals("PENDING") || estado.equals("PENDIENTE")) {
            estado = "PENDING";
        } else if (estado.equals("REJECTED") || estado.equals("RECHAZADO")
                || estado.equals("DECLINED") || estado.equals("DECLINADO")) {
            // Un rechazo no convierte una deuda en pagada; permanece pendiente.
            estado = "REJECTED";
        } else {
            throw new IllegalArgumentException("Estado PSE no soportado");
        }

        PagoWebhookEvento evento = new PagoWebhookEvento();
        evento.setEventoId(solicitud.eventoId().trim());
        evento.setReferenciaPago(solicitud.referenciaPago().trim());
        evento.setEstado(estado);
        evento.setRecibidoEn(LocalDateTime.now());
        eventoRepository.save(evento);
        return Resultado.PROCESADO;
    }

    private void validarSolicitud(PseWebhookRequest solicitud) {
        if (solicitud == null || !StringUtils.hasText(solicitud.eventoId())
                || !StringUtils.hasText(solicitud.referenciaPago())
                || !StringUtils.hasText(solicitud.estado()) || solicitud.monto() == null
                || solicitud.monto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El webhook PSE no tiene todos los datos requeridos");
        }
    }

    public enum Resultado {
        PROCESADO,
        DUPLICADO
    }

    public record PseWebhookRequest(String eventoId, String referenciaPago,
                                    String estado, BigDecimal monto) {
    }
}
