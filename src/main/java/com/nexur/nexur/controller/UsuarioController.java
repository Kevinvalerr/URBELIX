package com.nexur.nexur.controller;

import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.nexur.nexur.repository.ApartamentoRepository;
import java.util.List;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {
     
    private final ApartamentoRepository apartamentoRepository;
    private final UsuarioService usuarioService;

    public UsuarioController(ApartamentoRepository apartamentoRepository, UsuarioService usuarioService) {
        this.apartamentoRepository = apartamentoRepository;
        this.usuarioService = usuarioService;
    }

    // Mostrar lista
    @GetMapping
    public String listar(Model model) {
        List<Usuario> usuarios = usuarioService.listarUsuarios();
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("currentPath", "/usuarios");
        return "usuarios/lista";
    }

    // Formulario nuevo
   @GetMapping("/nuevo")
public String nuevo(Model model) {
    model.addAttribute("usuario", new Usuario());
    model.addAttribute("currentPath", "/usuarios");

    //  traer torres únicas desde BD
    List<String> torres = apartamentoRepository.findDistinctTorres();
    model.addAttribute("torres", torres);

    return "usuarios/nuevo";
}

    // AQUÍ ESTÁ EL CAMBIO IMPORTANTE
    @PostMapping("/guardar")
    public String guardar(
            @RequestParam String nombre,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String documento,
            @RequestParam String telefono,
            @RequestParam String numeroApartamento
    ) {

        usuarioService.crearUsuarioConResidente(
                nombre,
                email,
                password,
                documento,
                telefono,
                numeroApartamento
        );

        return "redirect:/usuarios";
    }

    // Editar
    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        Usuario usuario = usuarioService.buscarPorId(id);
        model.addAttribute("usuario", usuario);
        model.addAttribute("currentPath", "/usuarios");
        return "usuarios/editar";
    }

    // Actualizar (esto lo dejamos igual por ahora)
    @PostMapping("/actualizar/{id}")
    public String actualizar(@PathVariable Long id, @ModelAttribute Usuario usuario) {
        usuario.setId(id);
        usuarioService.guardarUsuario(usuario);
        return "redirect:/usuarios";
    }

    // Eliminar
    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id) {
        usuarioService.eliminarUsuario(id);
        return "redirect:/usuarios";
    }
}