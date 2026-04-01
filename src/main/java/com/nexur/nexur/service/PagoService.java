package com.nexur.nexur.service;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.Pago;
import com.nexur.nexur.repository.ApartamentoRepository;
import com.nexur.nexur.repository.PagoRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;

import java.util.List;

@Service
public class PagoService {

    private final PagoRepository pagoRepository;
    private final ApartamentoRepository apartamentoRepository;

    public PagoService(PagoRepository pagoRepository, ApartamentoRepository apartamentoRepository) {
        this.pagoRepository = pagoRepository;
        this.apartamentoRepository = apartamentoRepository;
    }

    public List<Pago> listarPagos() {
        return pagoRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    public List<Pago> obtenerUltimosPagos() {
        return pagoRepository.findTop4ByOrderByIdDesc();
    }

    public Pago guardar(Pago pago, Long apartamentoId) {
        Apartamento apartamento = apartamentoRepository.findById(apartamentoId)
                .orElseThrow(() -> new RuntimeException("Apartamento no encontrado"));
        pago.setApartamento(apartamento);
        return pagoRepository.save(pago);
    }

    public long contarPagos() {
        return pagoRepository.count();
    }
}
