package com.nexur.nexur.controller;

import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.model.Rol;
import com.nexur.nexur.service.UsuarioService;
import com.nexur.nexur.service.PagoService;
import com.nexur.nexur.service.NotificacionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.nexur.nexur.repository.ApartamentoRepository;
import java.util.List;
import com.nexur.nexur.service.ExcelExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {
     
    private final ApartamentoRepository apartamentoRepository;
    private final UsuarioService usuarioService;
    private final ExcelExportService excelExportService;
    private final PagoService pagoService;
    private final NotificacionService notificacionService;

    public UsuarioController(ApartamentoRepository apartamentoRepository, UsuarioService usuarioService,
                             ExcelExportService excelExportService, PagoService pagoService,
                             NotificacionService notificacionService) {
        this.apartamentoRepository = apartamentoRepository;
        this.usuarioService = usuarioService;
        this.excelExportService = excelExportService;
        this.pagoService = pagoService;
        this.notificacionService = notificacionService;
    }

    @GetMapping("/excel/residentes")
    public ResponseEntity<byte[]> exportarResidentes() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=residentes.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelExportService.exportarResidentes(usuarioService.listarUsuarios().stream()
                        .map(Usuario::getResidente)
                        .filter(java.util.Objects::nonNull)
                        .toList()));
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
            @RequestParam(defaultValue = "") String confirmPassword,
            @RequestParam(defaultValue = "RESIDENTE") Rol rol,
            @RequestParam(required = false) String documento,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String numeroApartamento,
            Model model
    ) {
        if (!password.equals(confirmPassword)) {
            Usuario usuario = new Usuario();
            usuario.setNombre(nombre);
            usuario.setEmail(email);
            usuario.setRol(rol);
            model.addAttribute("usuario", usuario);
            model.addAttribute("documento", documento);
            model.addAttribute("telefono", telefono);
            model.addAttribute("numeroApartamento", numeroApartamento);
            model.addAttribute("torres", apartamentoRepository.findDistinctTorres());
            model.addAttribute("formError", "Las contraseñas no coinciden");
            model.addAttribute("currentPath", "/usuarios");
            return "usuarios/nuevo";
        }
        try {
            Usuario creado = usuarioService.crearUsuario(nombre, email, password, rol,
                    documento, telefono, numeroApartamento);
            registrarObligacionInicial(creado);
        } catch (RuntimeException exception) {
            Usuario usuario = new Usuario();
            usuario.setNombre(nombre);
            usuario.setEmail(email);
            usuario.setRol(rol);
            model.addAttribute("usuario", usuario);
            model.addAttribute("documento", documento);
            model.addAttribute("telefono", telefono);
            model.addAttribute("numeroApartamento", numeroApartamento);
            model.addAttribute("torres", apartamentoRepository.findDistinctTorres());
            model.addAttribute("formError", exception.getMessage());
            model.addAttribute("currentPath", "/usuarios");
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
    public String actualizar(@PathVariable Long id, @ModelAttribute Usuario usuario,
                             RedirectAttributes redirectAttributes) {
        usuario.setId(id);
        try {
            usuarioService.guardarUsuarioActualizado(usuario);
            redirectAttributes.addFlashAttribute("success", "Usuario actualizado correctamente");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/usuarios/editar/" + id;
        }
        return "redirect:/usuarios";
    }

    private void registrarObligacionInicial(Usuario usuario) {
        if (usuario == null || usuario.getRol() != Rol.RESIDENTE || usuario.getResidente() == null) {
            return;
        }
        var pago = pagoService.crearObligacionInicial(usuario.getResidente());
        notificacionService.crear(usuario, "Obligación inicial registrada",
                "Administración registró tu obligación inicial por valor de " + pago.getMonto() + ".",
                "/pagos/" + pago.getId());
    }

    // Eliminar
    @PostMapping("/estado/{id}")
    public String cambiarEstado(@PathVariable Long id,
                                @RequestParam boolean activo,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            usuarioService.cambiarEstado(id, activo, authentication.getName());
            redirectAttributes.addFlashAttribute("success",
                    activo ? "Usuario activado correctamente" : "Usuario desactivado correctamente");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/usuarios";
    }
}
