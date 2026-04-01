package com.nexur.nexur.controller;

import com.nexur.nexur.model.Rol;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.service.UsuarioService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class VistaController {

    private final UsuarioService usuarioService;

    public VistaController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/usuarios-vista")
    public String mostrarUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioService.listarUsuarios());
        model.addAttribute("currentPath", "/usuarios-vista");
        model.addAttribute("volverUrl", "/dashboard");
        return "usuarios/lista";
    }

    @GetMapping("/usuarios/nuevo")
    public String mostrarFormularioNuevoUsuario(Model model) {
        model.addAttribute("currentPath", "/usuarios-vista");
        model.addAttribute("volverUrl", "/dashboard");
        return "usuarios/nuevo";
    }

    @PostMapping("/guardar-usuario")
    public String guardarUsuario(
        @RequestParam String nombre,
        @RequestParam String email,
        @RequestParam String password,
        @RequestParam String rol) {

        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        usuario.setPassword(password);
        usuario.setRol(Rol.valueOf(rol));

        usuarioService.guardarUsuario(usuario);

        return "redirect:/usuarios-vista";
    }

    @GetMapping("/eliminar-usuario")
    public String eliminarUsuario(@RequestParam Long id){
        usuarioService.eliminarUsuario(id);
        return "redirect:/usuarios-vista";
    }

    @GetMapping("/editar-usuario")
    public String mostrarFormularioEdicion(@RequestParam Long id, Model model){
        Usuario usuario = usuarioService.buscarPorId(id);

        model.addAttribute("usuario", usuario);
        model.addAttribute("volverUrl", "/dashboard");

        return "usuarios/editar";
    }

    @PostMapping("/actualizar-usuario")
    public String actualizarUsuario(
        @RequestParam Long id,
        @RequestParam String nombre,
        @RequestParam String email,
        @RequestParam(required = false) String password,
        @RequestParam String rol) {

        Usuario usuarioExistente = usuarioService.buscarPorId(id);
        if (usuarioExistente == null) {
            return "redirect:/usuarios-vista";
        }

        usuarioExistente.setNombre(nombre);
        usuarioExistente.setEmail(email);
        usuarioExistente.setRol(Rol.valueOf(rol));
        if (password != null && !password.trim().isEmpty()) {
            usuarioExistente.setPassword(password);
        }

        usuarioService.guardarUsuario(usuarioExistente);

        return "redirect:/usuarios-vista";
    }
    
}


