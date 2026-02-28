package com.nexur.nexur.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.nexur.nexur.service.UsuarioService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.nexur.nexur.model.Usuario;




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
     @PostMapping("/guardar-usuario")
 public String guardarUsuario(
    @RequestParam String nombre,
    @RequestParam String email,
    @RequestParam String password){


        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        usuario.setPassword(password);

        usuarioService.guardarUsuario(usuario);

        return "redirect:/usuarios-vista";
    }
}


