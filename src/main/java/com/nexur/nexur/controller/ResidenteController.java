package com.nexur.nexur.controller;


import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.service.ResidenteService;
import com.nexur.nexur.service.ApartamentoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller

@RequestMapping("/residentes")
public class ResidenteController {
  
     // Servicios que se usan
    private final ResidenteService residenteService;
    private final ApartamentoService apartamentoService;

    public ResidenteController(ResidenteService residenteService,
                               ApartamentoService  apartamentoService) {
    this.residenteService = residenteService;
    this.apartamentoService =apartamentoService;                                                                   
    }

    @GetMapping 
    public String listarResidentes(Model model) {
        
        model.addAttribute("residentes", residenteService.obtenerTodos());

        return "residentes/lista";
    }
    
    //Mostrar formulario para crear residente
    @GetMapping("/nuevo")
    public String mostrarFormulario(Model model) {
     
      Residente residente = new Residente();

      residente.setApartamento(new Apartamento());

      model.addAttribute("residente", residente);

      model.addAttribute("apartamentos", apartamentoService.listarApartamentos());
        
      return "residentes/formulario";

    }

    // Guardar residente

    @PostMapping("/guardar")
    public String guardarResidente(@ModelAttribute Residente residente,@RequestParam Long apartamentoId) {

        residenteService.guardar(residente, apartamentoId);

        return "redirect:/residentes";
    }

    //Eliminar residente
    @GetMapping("/eliminar/{id}")
    public String eliminarResidente(@PathVariable Long id) {

        residenteService.eliminar(id);

        return "redirect:/residentes";
    }


    
}

