<<<<<<< HEAD
package com.nexur.nexur.controller;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.service.UsuarioService;

@RestController
@RequestMapping("/usuarios")
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
=======
package com.nexur.nexur.controller;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.service.UsuarioService;

@RestController
@RequestMapping("/usuarios")
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
>>>>>>> 145629a3644a801db184b2f1f66436af732e08c4
}