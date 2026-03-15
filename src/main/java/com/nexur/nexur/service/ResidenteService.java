package com.nexur.nexur.service;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.repository.ResidenteRepository;
import com.nexur.nexur.repository.ApartamentoRepository;

import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Optional;

/*
Esta calse contiene la Lógica de negocio relacionado con los residentes.
El Service actúa como intermedio entre el controller y el Repository.
*/

@Service
public class ResidenteService {
      
     private final ApartamentoRepository apartamentoRepository;   
    // Inyectamos el repository para poder acceder a la base de datos
     private final ResidenteRepository residenteRepository;
    // Constructor para inyección de dependencias
     public ResidenteService(ResidenteRepository residenteRepository, ApartamentoRepository apartamentoRepository) {
        this.residenteRepository = residenteRepository;
        this.apartamentoRepository = apartamentoRepository;
     }

     public List<Residente> obtenerTodos() {
        return residenteRepository.findAll();
     }
   
     public Residente guardar(Residente residente,Long apartamentoId) {

    

      Apartamento apartamento = apartamentoRepository
             .findById(apartamentoId)
             .orElseThrow(() -> new RuntimeException("Apartamento no encontrado"));
       
             residente.setApartamento(apartamento);
      
             return residenteRepository.save(residente);
     }

     public Optional<Residente> buscarPorId(Long id) {
        return residenteRepository.findById(id);
     }

     public void eliminar(Long id) {
        residenteRepository.deleteById(id);
     }
}
