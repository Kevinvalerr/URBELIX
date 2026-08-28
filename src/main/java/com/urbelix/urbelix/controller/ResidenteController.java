package com.urbelix.urbelix.controller;


import com.urbelix.urbelix.model.Apartamento;
import com.urbelix.urbelix.model.Residente;
import com.urbelix.urbelix.model.Usuario;
import com.urbelix.urbelix.service.ResidenteService;
import com.urbelix.urbelix.service.ApartamentoService;
import com.urbelix.urbelix.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import org.springframework.security.access.prepost.PreAuthorize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

@Controller
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/residentes")
public class ResidenteController {

    private static final Logger log = LoggerFactory.getLogger(ResidenteController.class);
  
     // Servicios que se usan
    private final ResidenteService residenteService;
    private final ApartamentoService apartamentoService;
    private final UsuarioService usuarioService;

    public ResidenteController(ResidenteService residenteService,
                               ApartamentoService apartamentoService,
                               UsuarioService usuarioService) {
        this.residenteService = residenteService;
        this.apartamentoService = apartamentoService;
        this.usuarioService = usuarioService;
    }

    @GetMapping 
    public String listarResidentes(Model model) {
        model.addAttribute("residentes", residenteService.obtenerTodos());
        model.addAttribute("currentPath", "/residentes");
        model.addAttribute("volverUrl", "/dashboard");

        return "residentes/lista";
    }
    
    //Mostrar formulario para crear residente
    @GetMapping("/nuevo")
    public String mostrarFormulario(Model model) {
        Residente residente = new Residente();
        residente.setApartamento(new Apartamento());
        residente.setUsuario(new Usuario());

        model.addAttribute("residente", residente);
        model.addAttribute("apartamentos", apartamentoService.listarApartamentos());
        model.addAttribute("currentPath", "/residentes");
        model.addAttribute("volverUrl", "/residentes");
        model.addAttribute("titulo", "Registrar Residente");
        model.addAttribute("isNew", true);

        return "residentes/formulario";
    }

    @GetMapping("/editar/{id}")
    public String editarResidente(@PathVariable Long id, Model model) {
        Residente residente = residenteService.buscarPorId(id);

        if (residente.getApartamento() == null) {
            residente.setApartamento(new Apartamento());
        }
        if (residente.getUsuario() == null) {
            residente.setUsuario(new Usuario());
        }

        model.addAttribute("residente", residente);
        model.addAttribute("apartamentos", apartamentoService.listarApartamentos());
        model.addAttribute("currentPath", "/residentes");
        model.addAttribute("volverUrl", "/residentes");
        model.addAttribute("titulo", "Editar Residente");
        model.addAttribute("isNew", false);

        return "residentes/formulario";
    }

    // Guardar residente

    @PostMapping("/guardar")
    public String guardarResidente(@Valid @ModelAttribute Residente residente,
                                   BindingResult bindingResult,
                                   @RequestParam(name = "apartamentoIds", required = false) List<Long> apartamentoIds,
                                   Model model) {

        if (residente.getId() == null && (apartamentoIds == null || apartamentoIds.isEmpty())) {
            bindingResult.reject("Debe seleccionar al menos un apartamento");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("apartamentos", apartamentoService.listarApartamentos());
            model.addAttribute("currentPath", "/residentes");
            model.addAttribute("volverUrl", "/residentes");
            model.addAttribute("titulo", residente.getId() == null ? "Registrar Residente" : "Editar Residente");
            model.addAttribute("isNew", residente.getId() == null);
            return "residentes/formulario";
        }

        try {
            if (residente.getId() == null) {
                residenteService.crearConCuenta(residente, apartamentoIds);
            } else {
                residenteService.guardar(residente, apartamentoIds == null ? null : apartamentoIds.get(0));
            }
            return "redirect:/residentes";
        } catch (RuntimeException ex) {
            log.error("No se pudo guardar el residente", ex);
            model.addAttribute("apartamentos", apartamentoService.listarApartamentos());
            model.addAttribute("currentPath", "/residentes");
            model.addAttribute("volverUrl", "/residentes");
            model.addAttribute("titulo", residente.getId() == null ? "Registrar Residente" : "Editar Residente");
            model.addAttribute("isNew", residente.getId() == null);
            bindingResult.reject("No se pudo guardar el residente: " + ex.getMessage());
            return "residentes/formulario";
        }
    }

    //Eliminar residente
    @GetMapping("/eliminar/{id}")
    public String eliminarResidente(@PathVariable Long id) {

        residenteService.eliminar(id);

        return "redirect:/residentes";
    }


    
}

