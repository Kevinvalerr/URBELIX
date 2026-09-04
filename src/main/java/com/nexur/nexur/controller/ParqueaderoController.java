package com.nexur.nexur.controller;

import com.nexur.nexur.model.EstadoParqueadero;
import com.nexur.nexur.model.Parqueadero;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.model.Rol;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.repository.UsuarioRepository;
import com.nexur.nexur.service.ApartamentoService;
import com.nexur.nexur.service.ParqueaderoService;
import com.nexur.nexur.service.ResidenteService;
import com.nexur.nexur.service.NotificacionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.dao.DataIntegrityViolationException;

@Controller
@RequestMapping("/parqueaderos")
public class ParqueaderoController {

    private final ParqueaderoService parqueaderoService;
    private final ApartamentoService apartamentoService;
    private final ResidenteService residenteService;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;

    public ParqueaderoController(ParqueaderoService parqueaderoService,
                                 ApartamentoService apartamentoService,
                                 ResidenteService residenteService,
                                 UsuarioRepository usuarioRepository,
                                 NotificacionService notificacionService) {
        this.parqueaderoService = parqueaderoService;
        this.apartamentoService = apartamentoService;
        this.residenteService = residenteService;
        this.usuarioRepository = usuarioRepository;
        this.notificacionService = notificacionService;
    }

    @GetMapping
    public String listar(Model model, Authentication authentication) {
        boolean esResidente = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_RESIDENTE"));
        if (esResidente) {
            Residente residente;
            try {
                residente = residenteService.buscarPorUsuarioEmail(authentication.getName());
            } catch (RuntimeException exception) {
                residente = null;
            }
            if (residente == null || residente.getApartamento() == null || residente.getApartamento().getId() == null) {
                model.addAttribute("parqueaderos", java.util.List.of());
            } else {
                model.addAttribute("parqueaderos", parqueaderoService.listarPorApartamento(
                        residente.getApartamento().getId()));
            }
            model.addAttribute("esAdmin", false);
        } else {
            model.addAttribute("parqueaderos", parqueaderoService.listarTodos());
            model.addAttribute("esAdmin", true);
        }
        model.addAttribute("titulo", "Parqueaderos");
        model.addAttribute("currentPath", "/parqueaderos");
        model.addAttribute("volverUrl", "/dashboard");
        return "parqueaderos/lista";
    }

    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('ADMIN')")
    public String nuevo(Model model) {
        model.addAttribute("parqueadero", new Parqueadero());
        model.addAttribute("apartamentos", apartamentoService.listarApartamentos());
        model.addAttribute("estados", EstadoParqueadero.values());
        model.addAttribute("titulo", "Nuevo parqueadero");
        model.addAttribute("currentPath", "/parqueaderos");
        return "parqueaderos/formulario";
    }

    @GetMapping("/editar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("parqueadero", parqueaderoService.buscarPorId(id));
        model.addAttribute("apartamentos", apartamentoService.listarApartamentos());
        model.addAttribute("estados", EstadoParqueadero.values());
        model.addAttribute("titulo", "Editar parqueadero");
        model.addAttribute("currentPath", "/parqueaderos");
        return "parqueaderos/formulario";
    }

    @PostMapping("/guardar")
    @PreAuthorize("hasRole('ADMIN')")
    public String guardar(@ModelAttribute("parqueadero") Parqueadero parqueadero,
                          @RequestParam(required = false) Long apartamentoId,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        try {
            Parqueadero guardado = parqueaderoService.guardar(parqueadero, apartamentoId);
            notificarPorteria(guardado);
            redirectAttributes.addFlashAttribute("success", "Parqueadero guardado correctamente");
            return "redirect:/parqueaderos";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            model.addAttribute("apartamentos", apartamentoService.listarApartamentos());
            model.addAttribute("estados", EstadoParqueadero.values());
            model.addAttribute("titulo", "Nuevo parqueadero");
            model.addAttribute("currentPath", "/parqueaderos");
            return "parqueaderos/formulario";
        }
    }

    @PostMapping("/eliminar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            parqueaderoService.eliminar(id);
            redirectAttributes.addFlashAttribute("success", "Parqueadero eliminado correctamente");
        } catch (IllegalArgumentException | DataIntegrityViolationException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/parqueaderos";
    }

    private void notificarPorteria(Parqueadero parqueadero) {
        String mensaje = "Se actualizó el parqueadero " + parqueadero.getNumero()
                + ". Estado: " + parqueadero.getEstado() + ".";
        for (Usuario usuario : usuarioRepository.findByRolAndActivoTrue(Rol.PORTERIA)) {
            try {
                notificacionService.crear(usuario, "Parqueadero actualizado", mensaje,
                        "/porteria/parqueaderos");
            } catch (RuntimeException exception) {
                // La asignación ya fue persistida y no depende del correo.
            }
        }

        if (parqueadero.getApartamento() != null) {
            residenteService.obtenerTodos().stream()
                    .filter(residente -> residente.getApartamento() != null
                            && parqueadero.getApartamento().getId().equals(residente.getApartamento().getId()))
                    .map(Residente::getUsuario)
                    .filter(java.util.Objects::nonNull)
                    .forEach(usuario -> {
                        try {
                            notificacionService.crear(usuario, "Parqueadero actualizado",
                                    "El parqueadero " + parqueadero.getNumero()
                                            + " quedó asociado a tu apartamento.", "/parqueaderos");
                        } catch (RuntimeException exception) {
                            // El aviso es secundario frente a la persistencia de la asignación.
                        }
                    });
        }
    }
}
