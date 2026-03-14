package com.nexur.nexur.controller;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.service.ApartamentoService;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.ui.Model;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;



@Controller

public class ApartamentoController {
    @Autowired
    private ApartamentoService apartamentoService;


   @GetMapping("/apartamentos") 

   public String listarApartamentos(Model model) {

   List<Apartamento> apartamentos = apartamentoService.listarApartamentos();

    model.addAttribute("apartamentos", apartamentos);

    return "apartamentos";

   }

   

   @GetMapping("/apartamentos/editar/{id}")
   public String mostrarFormularioEditar(@PathVariable Long id, Model model) {

    Apartamento apartamento = apartamentoService.obtenerApartamentoPorId(id);

      model.addAttribute("apartamento", apartamento);

      return "editar-apartamento";

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

        return "crear-apartamento";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/apartamentos")
    public String guardarApartamento(@ModelAttribute("apartamento") Apartamento apartamento) {
         
        apartamentoService.guardarApartamento(apartamento);

        return "redirect:/apartamentos";

    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/apartamentos/eliminar/{id}")
     public String eliminarApartamento(@PathVariable Long id){
        
        apartamentoService.eliminarApartamento(id);

        return "redirect:/apartamentos";
     }   

    
}
