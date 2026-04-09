package com.nexur.nexur.controller;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.service.ApartamentoService;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.ui.Model;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;



@Controller
@PreAuthorize("hasRole('ADMIN')")
public class ApartamentoController {
    @Autowired
    private ApartamentoService apartamentoService;


   @GetMapping("/apartamentos") 

   public String listarApartamentos(Model model) {

   List<Apartamento> apartamentos = apartamentoService.listarApartamentos();

    model.addAttribute("apartamentos", apartamentos);
    model.addAttribute("titulo", "Apartamentos");
    model.addAttribute("currentPath", "/apartamentos");
    model.addAttribute("volverUrl", "/dashboard");

    return "apartamentos/lista";

   }

   

   @GetMapping("/apartamentos/editar/{id}")
   public String mostrarFormularioEditar(@PathVariable Long id, Model model) {

    Apartamento apartamento = apartamentoService.obtenerApartamentoPorId(id);

      model.addAttribute("apartamento", apartamento);
      model.addAttribute("titulo", "Editar Apartamento");
      model.addAttribute("currentPath", "/apartamentos");
      model.addAttribute("volverUrl", "/apartamentos");

      return "apartamentos/editar";

   }
 
   @PostMapping("/apartamentos/{id}")
   public String ActualizarApartamento(@PathVariable Long id,
                                       @ModelAttribute("apartamento") Apartamento apartamento) {
    Apartamento apartamentoExistente = apartamentoService.obtenerApartamentoPorId(id);
     
    apartamentoExistente.setNumero(apartamento.getNumero());
    apartamentoExistente.setTorre(apartamento.getTorre());
    apartamentoExistente.setPiso(apartamento.getPiso());
    apartamentoExistente.setEstado(apartamento.getEstado());


    apartamentoService.guardarApartamento(apartamentoExistente);
    

           return "redirect:/apartamentos";                           
         }
    
   
 
   
     @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/apartamentos/nuevo")
    public String mostrarFormularioCrear(Model model){
        Apartamento apartamento = new Apartamento();

        model.addAttribute("apartamento", apartamento);
        model.addAttribute("titulo", "Crear Apartamento");
        model.addAttribute("currentPath", "/apartamentos");
        model.addAttribute("volverUrl", "/apartamentos");

        return "apartamentos/crear";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/apartamentos")
    public String guardarApartamento(@ModelAttribute("apartamento") Apartamento apartamento) {
         
        apartamentoService.guardarApartamento(apartamento);

        return "redirect:/apartamentos";

    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/apartamentos/eliminar/{id}")
     public String eliminarApartamento(@PathVariable Long id, RedirectAttributes redirectAttributes){
        try {
            apartamentoService.eliminarApartamento(id);
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/apartamentos";
     }   

    
}
