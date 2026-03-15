package com.nexur.nexur.controller;

import org.springframework.web.bind.annotation.*;
import java.util.List;


import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.service.UsuarioService;

@RestController

@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService){
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public Usuario guardar(@RequestBody Usuario usuario){
        return usuarioService.guardarUsuario(usuario);
    }

    @GetMapping
    public List<Usuario> listar(){
        return usuarioService.listarUsuarios();
    }

   

}
