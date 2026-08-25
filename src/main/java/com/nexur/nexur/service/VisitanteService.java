package com.nexur.nexur.service;


import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.Visitante;
import com.nexur.nexur.repository.ApartamentoRepository;
import com.nexur.nexur.repository.VisitanteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service 
public class VisitanteService {


    @Autowired 
    private VisitanteRepository visitanteRepository;


    public List<Visitante> listarVisitantes() {
        return visitanteRepository.findAll();

    }

    public List<Visitante> listarVisitantesActivos() {
        return visitanteRepository.findByFechaSalidaIsNull();

    }

 @Autowired
 private ApartamentoRepository apartamentoRepository;

    public Visitante registrarEntrada(Visitante visitante) {
        
         Apartamento apto = apartamentoRepository
            .findById(visitante.getApartamento().getId())
            .orElse(null);

        visitante.setApartamento(apto);
        
        visitante.setFechaEntrada(LocalDateTime.now());
        return visitanteRepository.save(visitante);

    
    }

    public void registrarSalida(Long id) {

        Visitante visitante = visitanteRepository.findById(id).orElse(null);

        if (visitante != null) {

            visitante.setFechaSalida(LocalDateTime.now());

            visitanteRepository.save(visitante);
        }
    }

    public List<Visitante> buscarPorApartamento(Long apartamentoId){
        return visitanteRepository.findByApartamentoId(apartamentoId);
    }
    
}
