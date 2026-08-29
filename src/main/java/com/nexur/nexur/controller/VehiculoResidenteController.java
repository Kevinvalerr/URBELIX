package com.nexur.nexur.controller;

import com.nexur.nexur.model.TipoVehiculo;
import com.nexur.nexur.model.Vehiculo;
import com.nexur.nexur.service.VehiculoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/parqueaderos/mis-vehiculos")
@PreAuthorize("hasRole('RESIDENTE')")
public class VehiculoResidenteController {

    private final VehiculoService vehiculoService;

    public VehiculoResidenteController(VehiculoService vehiculoService) {
        this.vehiculoService = vehiculoService;
    }

    @GetMapping
    public String listar(Model model, Authentication authentication) {
        try {
            var residente = vehiculoService.buscarResidente(authentication.getName());
            model.addAttribute("vehiculos", vehiculoService.listarPorResidente(residente.getId()));
        } catch (IllegalArgumentException exception) {
            model.addAttribute("vehiculos", java.util.List.of());
            model.addAttribute("error", exception.getMessage());
        }
        model.addAttribute("titulo", "Mis vehículos");
        model.addAttribute("currentPath", "/parqueaderos/mis-vehiculos");
        model.addAttribute("volverUrl", "/parqueaderos");
        return "parqueaderos/mis-vehiculos";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        prepararFormulario(model, new Vehiculo(), "Registrar vehículo");
        return "parqueaderos/vehiculo-residente-formulario";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model, Authentication authentication) {
        try {
            prepararFormulario(model, vehiculoService.buscarParaResidente(id, authentication.getName()), "Editar vehículo");
            return "parqueaderos/vehiculo-residente-formulario";
        } catch (IllegalArgumentException exception) {
            return "redirect:/parqueaderos/mis-vehiculos";
        }
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("vehiculo") Vehiculo vehiculo,
                          Authentication authentication,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        try {
            vehiculoService.guardarParaResidente(vehiculo, authentication.getName());
            redirectAttributes.addFlashAttribute("success", "Vehículo guardado correctamente");
            return "redirect:/parqueaderos/mis-vehiculos";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            prepararFormulario(model, vehiculo, "Editar vehículo");
            return "parqueaderos/vehiculo-residente-formulario";
        }
    }

    private void prepararFormulario(Model model, Vehiculo vehiculo, String titulo) {
        model.addAttribute("vehiculo", vehiculo);
        model.addAttribute("tipos", TipoVehiculo.values());
        model.addAttribute("titulo", titulo);
        model.addAttribute("currentPath", "/parqueaderos/mis-vehiculos");
        model.addAttribute("volverUrl", "/parqueaderos/mis-vehiculos");
    }
}
