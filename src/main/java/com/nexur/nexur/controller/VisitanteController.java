package com.nexur.nexur.controller;

import com.nexur.nexur.model.Visitante;
import com.nexur.nexur.service.VisitanteService;
import com.nexur.nexur.service.ApartamentoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;




@Controller
@RequestMapping("/visitantes") //Ruta del modulo
public class VisitanteController {

    @Autowired
    private VisitanteService visitanteService;

/*
Se usa para mostrar la lista de apartamentos
*en el formulario de registro de visitantes.
*/
    @Autowired
    private ApartamentoService apartamentoService;

    /*
   Listar todos los visitantes registrados.
    */
    
   @GetMapping
   public String listarVisitantes(Model model){

    // Se obtienen todos los visitantes del sistema
    model.addAttribute("visitantes", visitanteService.listarVisitantes());
    model.addAttribute("currentPath", "/visitantes");
    model.addAttribute("volverUrl", "/dashboard");
    
    return "visitantes/listaVisitantes";

   }

  @GetMapping("/nuevo")
  public String mostrarFormulario(Model model) {
      // Objeto vistante vacío para el formulario
     model.addAttribute("visitante", new Visitante());
     // Lista de apartamentos para el select
     model.addAttribute("apartamentos", apartamentoService.listarApartamentos());
     model.addAttribute("currentPath", "/visitantes");
     model.addAttribute("volverUrl", "/visitantes");

      return "visitantes/formularioVisitante";
  }
  
  @PostMapping("/guardar")
  public String registrarVisitante(@Valid @ModelAttribute("visitante") Visitante visitante,
                                   BindingResult bindingResult,
                                   Model model) {
       /*
       El Service registrará automaticamente
       la fecha de entrada usando LocalDateTime.now()
       */
      if (visitante.getApartamento() == null || visitante.getApartamento().getId() == null) {
          bindingResult.rejectValue("apartamento", "NotNull", "Seleccione un apartamento");
      }
      if (bindingResult.hasErrors()) {
          model.addAttribute("apartamentos", apartamentoService.listarApartamentos());
          model.addAttribute("currentPath", "/visitantes");
          model.addAttribute("volverUrl", "/visitantes");
          return "visitantes/formularioVisitante";
      }

     visitanteService.registrarEntrada(visitante);
      
      return "redirect:/visitantes";
  }
  
  @GetMapping("/salida/{id}")
  public String registrarSalida(@PathVariable Long id) {
     // El service registra automáticamente la hora de la salida 
    visitanteService.registrarSalida(id);

      return "redirect:/visitantes";
  }
  

}
