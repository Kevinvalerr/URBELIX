package com.nexur.nexur.controller;

import com.nexur.nexur.model.EstadoMovimientoParqueadero;
import com.nexur.nexur.model.EstadoParqueadero;
import com.nexur.nexur.model.TipoVehiculo;
import java.time.LocalDate;
import com.nexur.nexur.service.MovimientoParqueaderoService;
import com.nexur.nexur.service.ParqueaderoService;
import com.nexur.nexur.service.VehiculoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/porteria/parqueaderos")
@PreAuthorize("hasRole('PORTERIA')")
public class MovimientoParqueaderoController {
    private final MovimientoParqueaderoService movimientoService;
    private final VehiculoService vehiculoService;
    private final ParqueaderoService parqueaderoService;

    public MovimientoParqueaderoController(MovimientoParqueaderoService movimientoService,
                                           VehiculoService vehiculoService,
                                           ParqueaderoService parqueaderoService) {
        this.movimientoService = movimientoService;
        this.vehiculoService = vehiculoService;
        this.parqueaderoService = parqueaderoService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("total", parqueaderoService.listarTodos().size());
        model.addAttribute("disponibles", movimientoService.contar(EstadoParqueadero.DISPONIBLE));
        model.addAttribute("ocupados", movimientoService.contar(EstadoParqueadero.OCUPADO));
        model.addAttribute("reservados", movimientoService.contar(EstadoParqueadero.RESERVADO));
        model.addAttribute("mantenimiento", movimientoService.contar(EstadoParqueadero.MANTENIMIENTO));
        model.addAttribute("vehiculosDentro", movimientoService.contarVehiculosDentro());
        model.addAttribute("movimientosActivos", movimientoService.listarActivos());
        model.addAttribute("currentPath", "/porteria/parqueaderos");
        model.addAttribute("titulo", "Parqueaderos");
        return "parqueaderos/dashboard";
    }

    @GetMapping("/ingreso")
    public String ingreso(Model model) {
        model.addAttribute("vehiculos", vehiculoService.listar());
        model.addAttribute("parqueaderos", parqueaderoService.listarTodos());
        model.addAttribute("currentPath", "/porteria/parqueaderos");
        model.addAttribute("titulo", "Registrar ingreso");
        return "parqueaderos/ingreso";
    }

    @PostMapping("/ingreso")
    public String registrarIngreso(@RequestParam Long vehiculoId, @RequestParam Long parqueaderoId,
                                   RedirectAttributes redirectAttributes) {
        try {
            movimientoService.registrarIngreso(vehiculoId, parqueaderoId);
            redirectAttributes.addFlashAttribute("success", "Ingreso registrado correctamente");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/porteria/parqueaderos";
    }

    @PostMapping("/salida/{id}")
    public String registrarSalida(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            movimientoService.registrarSalida(id);
            redirectAttributes.addFlashAttribute("success", "Salida registrada correctamente");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/porteria/parqueaderos";
    }

    @GetMapping("/historial")
    public String historial(@RequestParam(required = false) String placa,
                            @RequestParam(required = false) TipoVehiculo tipo,
                            @RequestParam(required = false) String parqueadero,
                            @RequestParam(required = false) EstadoMovimientoParqueadero estado,
                            @RequestParam(required = false) LocalDate fecha, Model model) {
        model.addAttribute("movimientos", movimientoService.filtrar(placa, tipo, parqueadero, estado, fecha));
        model.addAttribute("placa", placa);
        model.addAttribute("tipoSeleccionado", tipo);
        model.addAttribute("parqueadero", parqueadero);
        model.addAttribute("estadoSeleccionado", estado);
        model.addAttribute("fecha", fecha);
        model.addAttribute("tipos", TipoVehiculo.values());
        model.addAttribute("estadosMovimiento", EstadoMovimientoParqueadero.values());
        model.addAttribute("currentPath", "/porteria/parqueaderos");
        model.addAttribute("titulo", "Historial de movimientos");
        return "parqueaderos/historial";
    }
}
