package com.nexur.nexur.controller;

import com.nexur.nexur.model.TipoVehiculo;
import com.nexur.nexur.model.Vehiculo;
import com.nexur.nexur.service.ApartamentoService;
import com.nexur.nexur.service.ParqueaderoService;
import com.nexur.nexur.service.ResidenteService;
import com.nexur.nexur.service.VehiculoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/porteria/parqueaderos/vehiculos")
@PreAuthorize("hasRole('PORTERIA')")
public class VehiculoController {
    private final VehiculoService vehiculoService;
    private final ResidenteService residenteService;
    private final ParqueaderoService parqueaderoService;

    public VehiculoController(VehiculoService vehiculoService, ResidenteService residenteService,
                              ParqueaderoService parqueaderoService) {
        this.vehiculoService = vehiculoService;
        this.residenteService = residenteService;
        this.parqueaderoService = parqueaderoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("vehiculos", vehiculoService.listar());
        model.addAttribute("currentPath", "/porteria/parqueaderos/vehiculos");
        model.addAttribute("titulo", "Vehículos");
        return "parqueaderos/vehiculos";
    }

    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('PORTERIA')")
    public String nuevo(Model model) { return formulario(model, new Vehiculo(), "Registrar vehículo"); }

    @GetMapping("/editar/{id}")
    @PreAuthorize("hasRole('PORTERIA')")
    public String editar(@PathVariable Long id, Model model) {
        return formulario(model, vehiculoService.buscar(id), "Editar vehículo");
    }

    @PostMapping("/guardar")
    @PreAuthorize("hasRole('PORTERIA')")
    public String guardar(@ModelAttribute("vehiculo") Vehiculo vehiculo,
                          @RequestParam Long residenteId, @RequestParam(required = false) Long parqueaderoId,
                          Model model, RedirectAttributes redirectAttributes) {
        try {
            vehiculoService.guardar(vehiculo, residenteId, parqueaderoId);
            redirectAttributes.addFlashAttribute("success", "Vehículo guardado correctamente");
            return "redirect:/porteria/parqueaderos/vehiculos";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            return formulario(model, vehiculo, "Registrar vehículo");
        }
    }

    @PostMapping("/eliminar/{id}")
    @PreAuthorize("hasRole('PORTERIA')")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            vehiculoService.eliminar(id);
            redirectAttributes.addFlashAttribute("success", "Vehículo eliminado correctamente");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/porteria/parqueaderos/vehiculos";
    }

    private String formulario(Model model, Vehiculo vehiculo, String titulo) {
        model.addAttribute("vehiculo", vehiculo);
        model.addAttribute("residentes", residenteService.obtenerTodos());
        model.addAttribute("parqueaderos", parqueaderoService.listarTodos());
        model.addAttribute("tipos", TipoVehiculo.values());
        model.addAttribute("currentPath", "/porteria/parqueaderos/vehiculos");
        model.addAttribute("titulo", titulo);
        return "parqueaderos/vehiculo-formulario";
    }
}
