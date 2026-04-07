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
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/pagos")
public class PagoController {

    private final PagoService pagoService;
    private final ResidenteService residenteService;
    private final ApartamentoService apartamentoService;

    public PagoController(PagoService pagoService, ResidenteService residenteService, ApartamentoService apartamentoService) {
        this.pagoService = pagoService;
        this.residenteService = residenteService;
        this.apartamentoService = apartamentoService;
    }

    /**
     * Vista principal de pagos
     * ADMIN: ve todos los pagos
     * RESIDENTE: ve solo sus pagos
     */
    @GetMapping
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
        BigDecimal montoTotal = pagos.stream().map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("pagos", pagos);
        model.addAttribute("totalPagos", pagos.size());
        model.addAttribute("totalPendiente", totalPendiente);
        model.addAttribute("totalPagado", totalPagado);
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
            @RequestParam("residenteId") Long residenteId,
            @RequestParam("apartamentoId") Long apartamentoId,
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

        if (bindingResult.hasErrors()) {
            model.addAttribute("residentes", residenteService.obtenerTodos());
            model.addAttribute("apartamentos", apartamentoService.listarApartamentos());
            model.addAttribute("tiposPago", TipoPago.values());
            model.addAttribute("metodosPago", MetodoPago.values());
            model.addAttribute("titulo", "Registrar Nuevo Pago");
            model.addAttribute("currentPath", "/pagos/nuevo");
            model.addAttribute("volverUrl", "/pagos");
            return "pagos/nuevo";
        }

        try {
            // Asignar residente y apartamento
            Residente residente = residenteService.buscarPorId(residenteId);
            Apartamento apartamento = apartamentoService.obtenerApartamentoPorId(apartamentoId);

            pago.setResidente(residente);
            pago.setApartamento(apartamento);
            pago.setEstadoPago(EstadoPago.PENDIENTE);

            pagoService.guardar(pago, residenteId, apartamentoId);
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
            pagoService.generarPagosAdministracion();
            redirectAttributes.addFlashAttribute("success", "Administración generada exitosamente para todos los residentes");
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
    public String verDetallePago(@PathVariable Long id, Model model, Authentication authentication) {
        Pago pago = pagoService.buscarPorId(id);

        // Validar acceso: ADMIN ve todo, RESIDENTE solo ve sus pagos
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            String email = authentication.getName();
            Residente residente = residenteService.buscarPorUsuarioEmail(email);
            if (!pago.getResidente().getId().equals(residente.getId())) {
                throw new RuntimeException("No tienes permiso para ver este pago");
            }
        }

        model.addAttribute("pago", pago);
        model.addAttribute("titulo", "Detalle del Pago");
        model.addAttribute("currentPath", "/pagos");
        model.addAttribute("volverUrl", "/pagos");

        return "pagos/detalle";
    }

    /**
     * Confirmar pago - Cambiar estado a PAGADO (ADMIN y RESIDENTE)
     */
    @PostMapping("/{id}/confirmar")
    public String confirmarPago(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            Pago pago = pagoService.buscarPorId(id);

            // Validar acceso
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin) {
                String email = authentication.getName();
                Residente residente = residenteService.buscarPorUsuarioEmail(email);
                if (!pago.getResidente().getId().equals(residente.getId())) {
                    throw new RuntimeException("No tienes permiso para pagar este pago");
                }
            }

            // Solo se puede pagar pagos PENDIENTE
            if (pago.getEstadoPago() != EstadoPago.PENDIENTE) {
                redirectAttributes.addFlashAttribute("error", "Solo se pueden pagar pagos con estado PENDIENTE");
                return "redirect:/pagos";
            }

            pagoService.marcarComoPagado(id);
            redirectAttributes.addFlashAttribute("success", "Pago confirmado exitosamente");
            return "redirect:/pagos";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al confirmar pago: " + e.getMessage());
            return "redirect:/pagos";
        }
    }
}
