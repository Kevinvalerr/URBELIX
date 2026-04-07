package com.nexur.nexur.controller;


import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.service.ResidenteService;
import com.nexur.nexur.service.ApartamentoService;
import com.nexur.nexur.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import org.springframework.security.access.prepost.PreAuthorize;

@Controller
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/residentes")
public class ResidenteController {
  
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
                                   @RequestParam Long apartamentoId,
                                   @RequestParam(required = false) String confirmPassword,
                                   Model model) {

        if (residente.getId() == null) {
            if (residente.getUsuario() == null || residente.getUsuario().getEmail() == null || residente.getUsuario().getEmail().isBlank()) {
                bindingResult.rejectValue("usuario.email", "NotBlank", "El correo electrónico es obligatorio para el residente");
            }
            if (residente.getUsuario() == null || residente.getUsuario().getPassword() == null || residente.getUsuario().getPassword().isBlank()) {
                bindingResult.rejectValue("usuario.password", "NotBlank", "La contraseña es obligatoria para el residente");
            }
            if (confirmPassword == null || !confirmPassword.equals(residente.getUsuario().getPassword())) {
                bindingResult.rejectValue("usuario.password", "Match", "Las contraseñas no coinciden");
            }
            if (residente.getUsuario() != null && StringUtils.hasText(residente.getUsuario().getEmail()) && usuarioService.existePorEmail(residente.getUsuario().getEmail())) {
                bindingResult.rejectValue("usuario.email", "Duplicate", "Ya existe un usuario con ese correo");
            }
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("apartamentos", apartamentoService.listarApartamentos());
            model.addAttribute("currentPath", "/residentes");
            model.addAttribute("volverUrl", "/residentes");
            model.addAttribute("titulo", residente.getId() == null ? "Registrar Residente" : "Editar Residente");
            model.addAttribute("isNew", residente.getId() == null);
            return "residentes/formulario";
        }

        residenteService.guardar(residente, apartamentoId);

        return "redirect:/residentes";
    }

    //Eliminar residente
    @GetMapping("/eliminar/{id}")
    public String eliminarResidente(@PathVariable Long id) {

        residenteService.eliminar(id);

        return "redirect:/residentes";
    }


    
}

