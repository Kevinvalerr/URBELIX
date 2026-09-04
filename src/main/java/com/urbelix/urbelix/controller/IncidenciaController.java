package com.urbelix.urbelix.controller;

import com.urbelix.urbelix.model.Incidencia;
import com.urbelix.urbelix.model.Usuario;
import com.urbelix.urbelix.model.enums.*;
import com.urbelix.urbelix.service.ApartamentoService;
import com.urbelix.urbelix.service.IncidenciaService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Controller
@RequestMapping("/incidencias")
public class IncidenciaController {
    private final IncidenciaService service;
    private final ApartamentoService apartamentoService;
    public IncidenciaController(IncidenciaService service, ApartamentoService apartamentoService) { this.service = service; this.apartamentoService = apartamentoService; }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RESIDENTE','PORTERIA')")
    public String listar(@RequestParam(required=false) String texto, @RequestParam(required=false) EstadoIncidencia estado,
            @RequestParam(required=false) PrioridadIncidencia prioridad, @RequestParam(required=false) String torre,
            @RequestParam(required=false) String apartamento, @RequestParam(required=false) Long residenteId,
            @RequestParam(required=false) LocalDate desde, @RequestParam(required=false) LocalDate hasta,
            @RequestParam(required=false) String success, @RequestParam(required=false) String error,
            Authentication auth, Model model) {
        boolean propio = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_RESIDENTE"));
        model.addAttribute("incidencias", propio ? service.buscarPropias(auth.getName()) : service.buscar(texto, estado, prioridad, torre, apartamento, residenteId, desde, hasta));
        model.addAttribute("esPropio", propio); model.addAttribute("texto", texto); model.addAttribute("estado", estado); model.addAttribute("prioridad", prioridad); model.addAttribute("torre", torre); model.addAttribute("apartamento", apartamento); model.addAttribute("desde", desde); model.addAttribute("hasta", hasta); model.addAttribute("estados", EstadoIncidencia.values()); model.addAttribute("prioridades", PrioridadIncidencia.values()); model.addAttribute("torres", service.torres()); model.addAttribute("currentPath", "/incidencias");
        if (success != null) model.addAttribute("success", mensajeExito(success)); if (error != null) model.addAttribute("error", "No fue posible completar la operación.");
        if (!propio) { model.addAttribute("total", service.total()); model.addAttribute("pendientes", service.contar(EstadoIncidencia.PENDIENTE)); model.addAttribute("enProceso", service.contar(EstadoIncidencia.EN_PROCESO)); model.addAttribute("resueltas", service.contar(EstadoIncidencia.RESUELTA)); }
        return "incidencias/lista";
    }

    @GetMapping("/nueva") @PreAuthorize("hasAnyRole('ADMIN','RESIDENTE')")
    public String nueva(Authentication auth, Model model) { cargarFormulario(model, new Incidencia(), auth); return "incidencias/formulario"; }

    @GetMapping("/editar/{id}") @PreAuthorize("hasRole('ADMIN')")
    public String editar(@PathVariable Long id, Model model, Authentication auth) { cargarFormulario(model, service.obtener(id), auth); return "incidencias/formulario"; }

    @PostMapping("/guardar") @PreAuthorize("hasAnyRole('ADMIN','RESIDENTE')")
    public String guardar(@Valid @ModelAttribute("incidencia") Incidencia incidencia, BindingResult binding,
            @RequestParam(required=false) Long residenteId, @RequestParam(required=false) Long apartamentoId, Authentication auth, Model model) {
        try {
            if (binding.hasErrors()) { cargarFormulario(model, incidencia, auth); return "incidencias/formulario"; }
            if (residenteId == null && auth.getPrincipal() instanceof Usuario usuario && usuario.getResidente() != null) residenteId = usuario.getResidente().getId();
            boolean nueva = incidencia.getId() == null;
            if (nueva) service.crear(incidencia, residenteId, apartamentoId); else service.actualizar(incidencia, apartamentoId);
            return "redirect:/incidencias?success=" + (nueva ? "creada" : "actualizada");
        } catch (RuntimeException ex) { cargarFormulario(model, incidencia, auth); model.addAttribute("error", mensajeError(ex)); return "incidencias/formulario"; }
    }

    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','RESIDENTE','PORTERIA')")
    public String detalle(@PathVariable Long id, @RequestParam(required=false) String success, @RequestParam(required=false) String error, Authentication auth, Model model) { Incidencia incidencia = service.obtener(id); service.validarConsulta(incidencia, auth.getName()); model.addAttribute("incidencia", incidencia); model.addAttribute("success", success == null ? null : mensajeExito(success)); model.addAttribute("error", error == null ? null : "No fue posible completar la operación."); model.addAttribute("currentPath", "/incidencias"); return "incidencias/detalle"; }

    @PostMapping("/{id}/aceptar") @PreAuthorize("hasRole('ADMIN')") public String aceptar(@PathVariable Long id) { return cambiar(id, EstadoIncidencia.EN_PROCESO, null, "atendida"); }
    @PostMapping("/{id}/rechazar") @PreAuthorize("hasRole('ADMIN')") public String rechazar(@PathVariable Long id, @RequestParam String comentario) { return cambiar(id, EstadoIncidencia.RECHAZADA, comentario, "rechazada"); }
    @PostMapping("/{id}/resolver") @PreAuthorize("hasRole('ADMIN')") public String resolver(@PathVariable Long id, @RequestParam String comentario) { return cambiar(id, EstadoIncidencia.RESUELTA, comentario, "resuelta"); }
    @PostMapping("/{id}/eliminar") @PreAuthorize("hasRole('ADMIN')") public String eliminar(@PathVariable Long id) { try { service.eliminar(id); return "redirect:/incidencias?success=eliminada"; } catch (RuntimeException ex) { return "redirect:/incidencias?error=operacion"; } }

    private String cambiar(Long id, EstadoIncidencia estado, String comentario, String resultado) { try { service.cambiarEstado(id, estado, comentario); return "redirect:/incidencias/" + id + "?success=" + resultado; } catch (RuntimeException ex) { return "redirect:/incidencias/" + id + "?error=" + URLEncoder.encode(mensajeError(ex), StandardCharsets.UTF_8); } }
    private void cargarFormulario(Model model, Incidencia incidencia, Authentication auth) { model.addAttribute("incidencia", incidencia); model.addAttribute("prioridades", PrioridadIncidencia.values()); model.addAttribute("apartamentos", apartamentoService.listarApartamentos()); model.addAttribute("residentes", service.residentes()); model.addAttribute("residenteId", incidencia.getResidente() != null ? incidencia.getResidente().getId() : auth.getPrincipal() instanceof Usuario usuario && usuario.getResidente() != null ? usuario.getResidente().getId() : null); model.addAttribute("currentPath", "/incidencias"); model.addAttribute("volverUrl", "/incidencias"); }
    private String mensajeExito(String codigo) { return switch (codigo) { case "creada" -> "Incidencia registrada correctamente."; case "atendida" -> "Incidencia aceptada y puesta en proceso."; case "rechazada" -> "Incidencia rechazada correctamente."; case "resuelta" -> "Incidencia marcada como resuelta."; default -> "Operación realizada correctamente."; }; }
    private String mensajeError(RuntimeException ex) { return ex instanceof IllegalArgumentException || ex instanceof IllegalStateException ? ex.getMessage() : "No fue posible completar la operación."; }
}
