package com.nexur.nexur.controller;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.Pago;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.model.enums.EstadoPago;
import com.nexur.nexur.model.enums.MetodoPago;
import com.nexur.nexur.model.enums.TipoPago;
import com.nexur.nexur.service.ApartamentoService;
import com.nexur.nexur.service.PagoService;
import com.nexur.nexur.service.ResidenteService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.nexur.nexur.service.ExcelExportService;
import com.nexur.nexur.service.EstadoCuentaPdfService;
import com.nexur.nexur.service.FacturaPagoPdfService;
import com.nexur.nexur.service.PagoSimulacionService;
import com.nexur.nexur.service.NotificacionService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/pagos")
public class PagoController {

    private final PagoService pagoService;
    private final ResidenteService residenteService;
    private final ApartamentoService apartamentoService;
    private final ExcelExportService excelExportService;
    private final EstadoCuentaPdfService estadoCuentaPdfService;
    private final FacturaPagoPdfService facturaPagoPdfService;
    private final PagoSimulacionService pagoSimulacionService;
    private final NotificacionService notificacionService;

    private static final Logger log = LoggerFactory.getLogger(PagoController.class);

    public PagoController(PagoService pagoService, ResidenteService residenteService, ApartamentoService apartamentoService,
                          ExcelExportService excelExportService, EstadoCuentaPdfService estadoCuentaPdfService,
                          FacturaPagoPdfService facturaPagoPdfService,
                          PagoSimulacionService pagoSimulacionService,
                          NotificacionService notificacionService) {
        this.pagoService = pagoService;
        this.residenteService = residenteService;
        this.apartamentoService = apartamentoService;
        this.excelExportService = excelExportService;
        this.estadoCuentaPdfService = estadoCuentaPdfService;
        this.facturaPagoPdfService = facturaPagoPdfService;
        this.pagoSimulacionService = pagoSimulacionService;
        this.notificacionService = notificacionService;
    }

    @GetMapping("/excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportarExcel() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=pagos.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelExportService.exportarPagos(pagoService.listarPagos()));
    }

    @GetMapping("/estado-cuenta")
    @PreAuthorize("hasRole('RESIDENTE')")
    public ResponseEntity<byte[]> descargarEstadoCuenta(Authentication authentication) {
        try {
            Residente residente = residenteService.buscarPorUsuarioEmail(authentication.getName());
            byte[] pdf = estadoCuentaPdfService.generar(residente,
                    pagoService.listarPagosPorUsuario(authentication.getName()));
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=estado-cuenta.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (RuntimeException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping({"/{id}/pse/iniciar", "/{id}/iniciar"})
    @PreAuthorize("hasRole('RESIDENTE')")
    public String iniciarPagoPse(@PathVariable Long id,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            Pago pago = pagoService.iniciarPagoOnline(id, authentication.getName());
            redirectAttributes.addFlashAttribute("success", "Pago simulado preparado. Referencia: "
                    + pago.getReferenciaPago());
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/pagos";
        }
        return "redirect:/pagos/" + id + "/simulador";
    }

    @GetMapping("/{id}/factura")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENTE')")
    public ResponseEntity<byte[]> descargarFactura(@PathVariable Long id,
                                                   Authentication authentication) {
        try {
            Pago pago = pagoService.buscarPorId(id);
            validarAccesoPago(pago, authentication);
            byte[] pdf = facturaPagoPdfService.generar(pago);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=factura-pago-" + String.format("%06d", id) + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (RuntimeException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/simulador")
    @PreAuthorize("hasRole('RESIDENTE')")
    public String mostrarSimulador(@PathVariable Long id,
                                   Authentication authentication,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        try {
            Pago pago = pagoService.buscarPorId(id);
            validarAccesoPago(pago, authentication);
            if (!pagoSimulacionService.puedeSimular(pago)) {
                redirectAttributes.addFlashAttribute("error",
                        "Este pago todavía no está listo para el sandbox local");
                return "redirect:/pagos/" + id;
            }
            model.addAttribute("pago", pago);
            model.addAttribute("titulo", "Sandbox de pagos | Urbelix");
            model.addAttribute("currentPath", "/pagos");
            model.addAttribute("volverUrl", "/pagos/" + id);
            return "pagos/simulador";
        } catch (AccessDeniedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/pagos";
        }
    }

    @PostMapping("/{id}/simulador/resultado")
    @PreAuthorize("hasRole('RESIDENTE')")
    public String procesarSimulador(@PathVariable Long id,
                                    @RequestParam String estado,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        try {
            Pago pago = pagoService.buscarPorId(id);
            validarAccesoPago(pago, authentication);
            PagoSimulacionService.Resultado resultado = pagoSimulacionService.simular(
                    id, authentication.getName(), estado);
            String mensaje = switch (resultado.estadoProveedor()) {
                case "APPROVED" -> "La simulación local aprobó el pago y lo marcó como pagado.";
                case "PENDING" -> "La simulación local dejó el pago pendiente.";
                case "DECLINED" -> "La simulación local rechazó el pago; permanece pendiente.";
                case "VOIDED" -> "La simulación local anuló la transacción; permanece pendiente.";
                default -> "La simulación local registró un error; el pago permanece pendiente.";
            };
            redirectAttributes.addFlashAttribute("simulacionMensaje", mensaje);
            redirectAttributes.addFlashAttribute("simulacionEstado", resultado.estadoProveedor());
            redirectAttributes.addFlashAttribute("simulacionTransaccionId", resultado.transactionId());
            notificarResultadoSimulacion(pago, resultado);
        } catch (AccessDeniedException exception) {
            throw exception;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/pagos/" + id;
        }
        return "redirect:/pagos/" + id;
    }

    /**
     * Vista principal de pagos
     * ADMIN: ve todos los pagos
     * RESIDENTE: ve solo sus pagos
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENTE')")
    public String listarPagos(Model model, Authentication authentication) {
        String email = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<Pago> pagos;

        if (isAdmin) {
            pagos = pagoService.listarPagos();
            model.addAttribute("isAdmin", true);
        } else {
            pagos = pagoService.listarPagosPorUsuario(email);
            model.addAttribute("isAdmin", false);
        }

        // Estadísticas para el dashboard
        long totalPendiente = pagos.stream().filter(p -> p.getEstadoPago() == EstadoPago.PENDIENTE).count();
        long totalPagado = pagos.stream().filter(p -> p.getEstadoPago() == EstadoPago.PAGADO).count();
        long totalVencido = pagos.stream().filter(p -> p.getEstadoPago() == EstadoPago.VENCIDO).count();
        BigDecimal montoPendiente = pagos.stream()
            .filter(p -> p.getEstadoPago() == EstadoPago.PENDIENTE || p.getEstadoPago() == EstadoPago.VENCIDO)
            .map(Pago::getMonto)
            .filter(java.util.Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal montoTotal = pagos.stream()
            .map(Pago::getMonto)
            .filter(java.util.Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("pagos", pagos);
        model.addAttribute("totalPagos", pagos.size());
        model.addAttribute("totalPendiente", totalPendiente);
        model.addAttribute("totalPagado", totalPagado);
        model.addAttribute("totalVencido", totalVencido);
        model.addAttribute("montoPendiente", montoPendiente);
        model.addAttribute("montoTotal", montoTotal);
        model.addAttribute("titulo", "Gestión de Pagos");
        model.addAttribute("currentPath", "/pagos");

        return "pagos/lista";
    }

    /**
     * Formulario para crear nuevo pago (SOLO ADMIN)
     */
    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('ADMIN')")
    public String mostrarFormularioNuevoPago(Model model) {
        Pago pago = new Pago();
        pago.setFecha(LocalDate.now());
        pago.setFechaVencimiento(LocalDate.now().plusDays(30));
        pago.setTipoPago(TipoPago.ADMINISTRACION);
        pago.setMetodo(MetodoPago.TRANSFERENCIA);
        pago.setEstadoPago(EstadoPago.PENDIENTE);

        model.addAttribute("pago", pago);
        model.addAttribute("residentes", residenteService.obtenerTodos());
        model.addAttribute("apartamentos", apartamentoService.listarApartamentos());
        model.addAttribute("tiposPago", TipoPago.values());
        model.addAttribute("metodosPago", MetodoPago.values());
        model.addAttribute("titulo", "Registrar Nuevo Pago");
        model.addAttribute("currentPath", "/pagos/nuevo");
        model.addAttribute("volverUrl", "/pagos");

        return "pagos/nuevo";
    }

    /**
     * Guardar nuevo pago (SOLO ADMIN)
     */
    @PostMapping("/guardar")
    @PreAuthorize("hasRole('ADMIN')")
    public String guardarPago(
            @Valid @ModelAttribute("pago") Pago pago,
            BindingResult bindingResult,
            @RequestParam(value = "residenteId", required = false) Long residenteId,
            @RequestParam(value = "apartamentoId", required = false) Long apartamentoId,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Validaciones
        if (residenteId == null || residenteId <= 0) {
            bindingResult.rejectValue("residente", "error.residente", "Debe seleccionar un residente");
        }
        if (apartamentoId == null || apartamentoId <= 0) {
            bindingResult.rejectValue("apartamento", "error.apartamento", "Debe seleccionar un apartamento");
        }
        if (pago.getMonto() == null || pago.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            bindingResult.rejectValue("monto", "error.monto", "El monto debe ser mayor a cero");
        }
        if (pago.getTipoPago() == null) {
            bindingResult.rejectValue("tipoPago", "error.tipoPago", "Debe seleccionar tipo de pago");
        }
        if (pago.getMetodo() == null) {
            bindingResult.rejectValue("metodo", "error.metodo", "Debe seleccionar método de pago");
        }
        if (pago.getFecha() != null && pago.getFechaVencimiento() != null
                && pago.getFechaVencimiento().isBefore(pago.getFecha())) {
            bindingResult.rejectValue("fechaVencimiento", "error.fechaVencimiento",
                    "La fecha de vencimiento no puede ser anterior a la fecha de emisión");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("residentes", residenteService.obtenerTodos());
            model.addAttribute("apartamentos", apartamentoService.listarApartamentos());
            model.addAttribute("tiposPago", TipoPago.values());
            model.addAttribute("metodosPago", MetodoPago.values());
            model.addAttribute("titulo", "Registrar Nuevo Pago");
            model.addAttribute("currentPath", "/pagos/nuevo");
            model.addAttribute("volverUrl", "/pagos");
            model.addAttribute("selectedResidenteId", residenteId);
            model.addAttribute("selectedApartamentoId", apartamentoId);
            return "pagos/nuevo";
        }

        try {
            // Asignar residente y apartamento
            Residente residente = residenteService.buscarPorId(residenteId);
            Apartamento apartamento = apartamentoService.obtenerApartamentoPorId(apartamentoId);

            pago.setResidente(residente);
            pago.setApartamento(apartamento);
            pago.setEstadoPago(EstadoPago.PENDIENTE);

            Pago guardado = pagoService.guardar(pago, residenteId, apartamentoId);
            crearNotificacionPago(guardado, "Nueva obligación de pago",
                    "Se registró una nueva obligación de pago por valor de " + guardado.getMonto() + ".");
            redirectAttributes.addFlashAttribute("success", "Pago registrado exitosamente");
            return "redirect:/pagos";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al registrar pago: " + e.getMessage());
            return "redirect:/pagos/nuevo";
        }
    }

    /**
     * Genera pagos de administración para todos los residentes (SOLO ADMIN)
     */
    @PostMapping("/generar-administracion")
    @PreAuthorize("hasRole('ADMIN')")
    public String generarAdministracion(RedirectAttributes redirectAttributes) {
        try {
            List<Pago> generados = pagoService.generarPagosAdministracion();
            generados.forEach(pago -> crearNotificacionPago(pago, "Nueva cuota de administración",
                    "Se generó tu obligación mensual de administración por valor de " + pago.getMonto() + "."));
            redirectAttributes.addFlashAttribute("success", "Administración generada para "
                    + generados.size() + " residente(s)");
            return "redirect:/pagos";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al generar administración: " + e.getMessage());
            return "redirect:/pagos";
        }
    }

    /**
     * Vista detalle del pago para confirmar pago (ADMIN y RESIDENTE)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENTE')")
    public String verDetallePago(@PathVariable Long id,
                                 Model model,
                                 Authentication authentication) {
        Pago pago = pagoService.buscarPorId(id);

        validarAccesoPago(pago, authentication);

        model.addAttribute("pago", pago);
        model.addAttribute("titulo", "Detalle del Pago");
        model.addAttribute("currentPath", "/pagos");
        model.addAttribute("volverUrl", "/pagos");
        model.addAttribute("simulacionHabilitada", pagoSimulacionService.puedeSimular(pago));
        model.addAttribute("pagoIniciado", StringUtils.hasText(pago.getReferenciaPago()));

        return "pagos/detalle";
    }

    private void validarAccesoPago(Pago pago, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return;
        }
        try {
            Residente residente = residenteService.buscarPorUsuarioEmail(authentication.getName());
            if (pago.getResidente() == null || residente == null
                    || !pago.getResidente().getId().equals(residente.getId())) {
                throw new AccessDeniedException("No tienes permiso para ver este pago");
            }
        } catch (AccessDeniedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AccessDeniedException("No tienes permiso para ver este pago");
        }
    }

    /**
     * Confirmar pago recibido por transferencia o efectivo (SOLO ADMIN)
     */
    @PostMapping("/{id}/confirmar")
    @PreAuthorize("hasRole('ADMIN')")
    public String confirmarPago(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            Pago pago = pagoService.buscarPorId(id);

            // Una deuda vencida sigue siendo cobrable y puede regularizarse.
            if (pago.getEstadoPago() != EstadoPago.PENDIENTE
                    && pago.getEstadoPago() != EstadoPago.VENCIDO) {
                redirectAttributes.addFlashAttribute("error", "Solo se pueden confirmar pagos pendientes o vencidos");
                return "redirect:/pagos";
            }

            pagoService.marcarComoPagado(id);
            notificarPagoConfirmado(pago);
            redirectAttributes.addFlashAttribute("success", "Pago confirmado exitosamente");
            return "redirect:/pagos";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al confirmar pago: " + e.getMessage());
            return "redirect:/pagos";
        }
    }

    private void notificarResultadoSimulacion(Pago pago, PagoSimulacionService.Resultado resultado) {
        String mensaje = switch (resultado.estadoProveedor()) {
            case "APPROVED" -> "Tu pago simulado fue aprobado y quedó registrado como pagado.";
            case "PENDING" -> "Tu pago simulado continúa pendiente de confirmación.";
            case "DECLINED" -> "Tu pago simulado fue rechazado y la obligación continúa pendiente.";
            case "VOIDED" -> "Tu pago simulado fue anulado y la obligación continúa pendiente.";
            default -> "La simulación del pago terminó con un error.";
        };
        crearNotificacionPago(pago, "Resultado de pago", mensaje
                + " Transacción: " + resultado.transactionId() + ".");
    }

    private void notificarPagoConfirmado(Pago pago) {
        crearNotificacionPago(pago, "Pago confirmado", "Administración confirmó tu pago y registró la fecha efectiva.");
    }

    private void crearNotificacionPago(Pago pago, String titulo, String mensaje) {
        try {
            if (pago != null && pago.getResidente() != null && pago.getResidente().getUsuario() != null) {
                notificacionService.crear(pago.getResidente().getUsuario(), titulo, mensaje,
                        "/pagos/" + pago.getId());
            }
        } catch (RuntimeException exception) {
            log.warn("No se pudo crear la notificacion del pago {}", pago == null ? null : pago.getId(), exception);
        }
    }
}
