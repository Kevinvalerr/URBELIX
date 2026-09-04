package com.urbelix.urbelix.controller;

import com.urbelix.urbelix.model.Usuario;
import com.urbelix.urbelix.model.Rol;
import com.urbelix.urbelix.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.urbelix.urbelix.repository.ApartamentoRepository;
import java.util.List;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private static final Logger log = LoggerFactory.getLogger(UsuarioController.class);
     
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
    model.addAttribute("roles", java.util.Arrays.stream(Rol.values())
        .filter(rol -> rol != Rol.RESIDENTE)
        .toList());
    model.addAttribute("currentPath", "/usuarios");

    //  traer torres únicas desde BD
    List<String> torres = apartamentoRepository.findDistinctTorres();
    model.addAttribute("torres", torres);

    return "usuarios/nuevo";
}

    // AQUÍ ESTÁ EL CAMBIO IMPORTANTE
    @PostMapping("/guardar")
    public String guardar(
            @Valid @ModelAttribute("usuario") Usuario usuario,
            BindingResult bindingResult,
            @RequestParam(required = false) String confirmPassword,
            @RequestParam(required = false) String documento,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String numeroApartamento,
            Model model
    ) {
        if (usuario.getRol() == null) {
            bindingResult.reject("Debe seleccionar un rol");
        }
        if (confirmPassword == null || !confirmPassword.equals(usuario.getPassword())) {
            bindingResult.reject("Las contraseñas no coinciden");
        }
        if (usuario.getRol() == Rol.RESIDENTE) {
            bindingResult.reject("Los usuarios RESIDENTE se crean desde Residentes");
        }
        if (usuarioService.existePorEmail(usuario.getEmail())) {
            bindingResult.reject("Ya existe un usuario con ese correo");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("formError", bindingResult.getAllErrors().get(0).getDefaultMessage());
            model.addAttribute("documento", documento);
            model.addAttribute("telefono", telefono);
            model.addAttribute("numeroApartamento", numeroApartamento);
                model.addAttribute("roles", java.util.Arrays.stream(Rol.values())
                    .filter(rol -> rol != Rol.RESIDENTE)
                    .toList());
            model.addAttribute("currentPath", "/usuarios");
            model.addAttribute("torres", apartamentoRepository.findDistinctTorres());
            return "usuarios/nuevo";
        }

        try {
            usuarioService.crearUsuarioConResidente(
                    usuario.getNombre(), usuario.getEmail(), usuario.getPassword(),
                    documento, telefono, numeroApartamento, usuario.getRol());
        } catch (RuntimeException ex) {
            log.error("No se pudo crear el usuario y su residente asociado", ex);
            model.addAttribute("formError", "No se pudo crear el usuario: " + ex.getMessage());
            model.addAttribute("documento", documento);
            model.addAttribute("telefono", telefono);
            model.addAttribute("numeroApartamento", numeroApartamento);
            model.addAttribute("roles", Rol.values());
            model.addAttribute("currentPath", "/usuarios");
            model.addAttribute("torres", apartamentoRepository.findDistinctTorres());
            return "usuarios/nuevo";
        }

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
        usuarioService.guardarUsuarioActualizado(usuario);
        return "redirect:/usuarios";
    }

    // Eliminar
    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id) {
        usuarioService.eliminarUsuario(id);
        return "redirect:/usuarios";
    }
}