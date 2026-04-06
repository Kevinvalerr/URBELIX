package com.nexur.nexur.controller;

import com.nexur.nexur.model.Pago;
import com.nexur.nexur.model.Reserva;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.service.ApartamentoService;
import com.nexur.nexur.service.PagoService;
import com.nexur.nexur.service.ReservaService;
import com.nexur.nexur.service.ResidenteService;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;


@Controller
public class PagoReservaController {

    private final PagoService pagoService;
    private final ReservaService reservaService;
    private final ApartamentoService apartamentoService;
    private final ResidenteService residenteService;

    public PagoReservaController(PagoService pagoService, ReservaService reservaService, ApartamentoService apartamentoService, ResidenteService residenteService) {
        this.pagoService = pagoService;
        this.reservaService = reservaService;
        this.apartamentoService = apartamentoService;
        this.residenteService = residenteService;
    }

 

    @GetMapping("/pagos/nuevo")
public String nuevoPago(Model model, Authentication authentication) {

    String email = authentication.getName();

    // 🔹 obtener residente del usuario logeado
    Residente residente = residenteService.buscarPorUsuarioEmail(email);

    Pago pago = new Pago();
    pago.setResidente(residente);
    pago.setApartamento(residente.getApartamento());

    model.addAttribute("pago", pago);

    return "pagos/nuevo";
}
@PostMapping("/pagos/guardar")
public String guardarPago(@Valid @ModelAttribute("pago") Pago pago,
                         BindingResult bindingResult,
                         Model model,
                         @RequestParam("residenteId") Long residenteId) {

    System.out.println("RESIDENTE ID CONTROLLER: " + residenteId);

    // cargar residente real
    Residente residente = residenteService.buscarPorId(residenteId);
    pago.setResidente(residente);

    if (bindingResult.hasErrors()) {
        model.addAttribute("apartamentos", apartamentoService.listarApartamentos());
        model.addAttribute("residentes", residenteService.obtenerTodos());
        return "pagos/nuevo";
    }

    pagoService.guardar(pago, pago.getApartamento().getId());
    return "redirect:/pagos";
}

    @GetMapping("/reservas")
    public String mostrarListaReservas(Model model) {
        model.addAttribute("titulo", "Reservas");
        model.addAttribute("currentPath", "/reservas");
        model.addAttribute("volverUrl", "/dashboard");
        model.addAttribute("reservas", reservaService.listarReservas());
        return "reservas/lista";
    }

    @GetMapping("/reservas/nueva")
    public String mostrarFormularioReserva(Model model) {
        model.addAttribute("titulo", "Crear Reserva");
        model.addAttribute("currentPath", "/reservas/nueva");
        model.addAttribute("volverUrl", "/dashboard");
        model.addAttribute("apartamentos", apartamentoService.listarApartamentos());
        model.addAttribute("reserva", new Reserva());
        return "reservas/nueva";
    }

   @PostMapping("/reservas/guardar")
public String guardarReserva(@Valid @ModelAttribute("reserva") Reserva reserva,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes,
                             Authentication authentication) {

    if (reserva.getApartamento() == null || reserva.getApartamento().getId() == null) {
        bindingResult.rejectValue("apartamento", "NotNull", "Seleccione un apartamento");
    }

    if (bindingResult.hasErrors()) {
        model.addAttribute("titulo", "Crear Reserva");
        model.addAttribute("apartamentos", apartamentoService.listarApartamentos());
        return "reservas/nueva";
    }
String email = authentication.getName();
Residente residente = residenteService.obtenerTodos().stream()
    .filter(r -> r.getNombre().equalsIgnoreCase(email))
    .findFirst()
    .orElseGet(() -> {
        Residente nuevo = new Residente();
        nuevo.setNombre(email);
        return residenteService.guardar(nuevo, reserva.getApartamento().getId());
    });
reserva.setResidente(residente);

    reservaService.guardar(reserva, reserva.getApartamento().getId());

    
    redirectAttributes.addFlashAttribute("success", "Reserva creada exitosamente");

    return "redirect:/reservas";
}

@PreAuthorize("hasAuthority('ADMIN')")
@PostMapping("/reservas/aprobar/{id}")
public String aprobar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
    reservaService.aprobarReserva(id);
    redirectAttributes.addFlashAttribute("success", "Reserva aprobada");
    return "redirect:/reservas";
}

@PreAuthorize("hasAuthority('ADMIN')")
@PostMapping("/reservas/rechazar/{id}")
public String rechazar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
    reservaService.rechazarReserva(id);
    redirectAttributes.addFlashAttribute("error", "Reserva rechazada");
    return "redirect:/reservas";
}

@GetMapping("/pagos/pagar/{id}")
public String mostrarPago(@PathVariable Long id, Model model) {
    Pago pago = pagoService.buscarPorId(id);
    model.addAttribute("pago", pago);
    return "pagos/pagar";
}

@PostMapping("/pagos/confirmar")
public String confirmarPago(@RequestParam Long id) {
    pagoService.marcarComoPagado(id);
    return "redirect:/pagos";
}

@PostMapping("/pagos/generar")
public String generarPagos() {
    pagoService.generarPagosAdministracion();
    return "redirect:/pagos";
}
@GetMapping("/pagos")
public String listarPagos(Model model, Authentication authentication) {

    String email = authentication.getName();

    boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

    List<Pago> pagos;

    if (isAdmin) {
        pagos = pagoService.listarPagos();
    } else {
        pagos = pagoService.listarPagosPorUsuario(email);
    }

    model.addAttribute("pagos", pagos);
    model.addAttribute("currentPath", "/pagos");

    return "pagos/lista";
}
}
