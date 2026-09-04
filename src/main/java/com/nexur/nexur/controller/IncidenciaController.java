package com.nexur.nexur.controller;

import com.nexur.nexur.model.EstadoIncidencia;
import com.nexur.nexur.model.Incidencia;
import com.nexur.nexur.model.IncidenciaAdjunto;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.service.ArchivoStorageService;
import com.nexur.nexur.service.IncidenciaService;
import com.nexur.nexur.service.ResidenteService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/incidencias")
public class IncidenciaController {

    private final IncidenciaService incidenciaService;
    private final ResidenteService residenteService;
    private final ArchivoStorageService archivoStorageService;

    public IncidenciaController(IncidenciaService incidenciaService, ResidenteService residenteService,
                                ArchivoStorageService archivoStorageService) {
        this.incidenciaService = incidenciaService;
        this.residenteService = residenteService;
        this.archivoStorageService = archivoStorageService;
    }

    @GetMapping
    public String listar(Model model, Authentication authentication) {
        boolean esAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        List<Incidencia> incidencias;
        if (esAdmin) {
            incidencias = incidenciaService.listarTodas();
        } else {
            Residente residente = residenteService.buscarPorUsuarioEmail(authentication.getName());
            incidencias = incidenciaService.listarPorResidente(residente.getId());
        }
        model.addAttribute("incidencias", incidencias);
        model.addAttribute("estados", EstadoIncidencia.values());
        model.addAttribute("currentPath", "/incidencias");
        model.addAttribute("titulo", "Incidencias | Urbelix");
        model.addAttribute("volverUrl", "/dashboard");
        return "incidencias/lista";
    }

    @GetMapping("/nueva")
    @PreAuthorize("hasRole('RESIDENTE')")
    public String nueva(Model model) {
        model.addAttribute("incidencia", new Incidencia());
        model.addAttribute("currentPath", "/incidencias/nueva");
        model.addAttribute("titulo", "Nueva incidencia | Urbelix");
        model.addAttribute("volverUrl", "/incidencias");
        return "incidencias/nueva";
    }

    @PostMapping
    @PreAuthorize("hasRole('RESIDENTE')")
    public String crear(@Valid @ModelAttribute("incidencia") Incidencia incidencia,
                        BindingResult bindingResult,
                        Authentication authentication,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("currentPath", "/incidencias/nueva");
            model.addAttribute("titulo", "Nueva incidencia | Urbelix");
            model.addAttribute("volverUrl", "/incidencias");
            return "incidencias/nueva";
        }
        try {
            incidenciaService.crear(incidencia, residenteService.buscarPorUsuarioEmail(authentication.getName()));
            redirectAttributes.addFlashAttribute("success", "Incidencia registrada correctamente");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/incidencias";
    }

    @PostMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public String actualizarEstado(@PathVariable Long id,
                                   @RequestParam EstadoIncidencia estado,
                                   @RequestParam(required = false) String respuesta,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            incidenciaService.actualizarEstado(id, estado, respuesta, authentication.getName());
            redirectAttributes.addFlashAttribute("success", "Incidencia actualizada");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/incidencias";
    }

    @PostMapping("/{id}/comentarios")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENTE')")
    public String comentar(@PathVariable Long id,
                           @RequestParam String contenido,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        boolean esAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        try {
            incidenciaService.agregarComentario(id, contenido, authentication.getName(),
                    authentication.getName(), esAdmin);
            redirectAttributes.addFlashAttribute("success", "Comentario agregado");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/incidencias";
    }

    @PostMapping("/{id}/adjuntos")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENTE')")
    public String adjuntar(@PathVariable Long id,
                           @RequestParam("archivo") MultipartFile archivo,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        boolean esAdmin = esAdmin(authentication);
        try {
            incidenciaService.agregarAdjunto(id, archivo, authentication.getName(), esAdmin);
            redirectAttributes.addFlashAttribute("success", "Evidencia adjuntada correctamente");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/incidencias";
    }

    @GetMapping("/{id}/adjuntos/{adjuntoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENTE')")
    public ResponseEntity<Resource> descargar(@PathVariable Long id,
                                              @PathVariable Long adjuntoId,
                                              Authentication authentication) {
        try {
            boolean esAdmin = esAdmin(authentication);
            IncidenciaAdjunto adjunto = incidenciaService.buscarAdjunto(id, adjuntoId,
                    authentication.getName(), esAdmin);
            Resource recurso = archivoStorageService.cargar(adjunto.getNombreInterno());
            MediaType tipo = MediaType.APPLICATION_OCTET_STREAM;
            try {
                tipo = MediaType.parseMediaType(adjunto.getTipoContenido());
            } catch (InvalidMediaTypeException ignored) {
                // Se conserva un tipo seguro si el metadato fue alterado.
            }
            ContentDisposition disposicion = ContentDisposition.attachment()
                    .filename(adjunto.getNombreOriginal(), StandardCharsets.UTF_8)
                    .build();
            return ResponseEntity.ok()
                    .contentType(tipo)
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                    .body(recurso);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    private boolean esAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
