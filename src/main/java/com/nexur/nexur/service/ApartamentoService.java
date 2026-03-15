package com.nexur.nexur.service;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.repository.ApartamentoRepository;

import org.springframework.stereotype.Service;
import java.util.List;

@Service

public class ApartamentoService {
    
    private final ApartamentoRepository apartamentoRepository;
     public ApartamentoService(ApartamentoRepository apartamentoRepository){
        this.apartamentoRepository = apartamentoRepository;
     }

    public List<Apartamento> listarApartamentos(){

    return apartamentoRepository.findAll();
    }

    public void guardarApartamento(Apartamento apartamento){
      
    apartamentoRepository.save(apartamento);    
    }

    public Apartamento obtenerApartamentoPorId(Long id){
        return apartamentoRepository.findById(id).orElse(null);

    }

    public void eliminarApartamento(Long id){

        apartamentoRepository.deleteById(id);
    }

    
}
