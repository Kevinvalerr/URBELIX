package com.nexur.nexur.controller;

import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/admin") // 🔐 protegido por ADMIN
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // 📄 Mostrar lista de usuarios (HTML)
    @GetMapping ("/usuarios-vista")
    public String listar(Model model) {

        List<Usuario> usuarios = usuarioService.listarUsuarios();

        model.addAttribute("usuarios", usuarios);

        //  busca: templates/usuarios/lista.html
        return "usuarios/lista";
    }

    @GetMapping("/debug")
@ResponseBody
public String debug(Authentication auth) {
    return auth.getAuthorities().toString();
}
}