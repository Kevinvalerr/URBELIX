package com.nexur.nexur.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.nexur.nexur.service.UsuarioService;

@Controller
public class VistaController {

    private final UsuarioService usuarioService;

    public VistaController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/usuarios-vista")
    public String mostrarUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioService.listarUsuarios());
        return "usuarios";
    }
}