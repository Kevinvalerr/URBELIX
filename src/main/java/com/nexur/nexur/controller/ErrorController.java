package com.nexur.nexur.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ErrorController {

    @GetMapping("/acceso-denegado")
    public String accesoDenegado(Model model) {
        model.addAttribute("titulo", "Acceso restringido");
        model.addAttribute("currentPath", "/acceso-denegado");
        model.addAttribute("volverUrl", "/dashboard");
        return "error/acceso-denegado";
    }
}
