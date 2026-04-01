package com.nexur.nexur.service;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.Reserva;
import com.nexur.nexur.repository.ApartamentoRepository;
import com.nexur.nexur.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;

import java.util.List;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final ApartamentoRepository apartamentoRepository;

    public ReservaService(ReservaRepository reservaRepository, ApartamentoRepository apartamentoRepository) {
        this.reservaRepository = reservaRepository;
        this.apartamentoRepository = apartamentoRepository;
    }

    public List<Reserva> listarReservas() {
        return reservaRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    public List<Reserva> obtenerUltimasReservas() {
        return reservaRepository.findTop4ByOrderByIdDesc();
    }

    public Reserva guardar(Reserva reserva, Long apartamentoId) {
        Apartamento apartamento = apartamentoRepository.findById(apartamentoId)
                .orElseThrow(() -> new RuntimeException("Apartamento no encontrado"));
        reserva.setApartamento(apartamento);
        if (reserva.getEstado() == null || reserva.getEstado().isBlank()) {
            reserva.setEstado("Pendiente");
        }
        return reservaRepository.save(reserva);
    }

    public long contarReservas() {
        return reservaRepository.count();
    }

    public long contarReservasPendientes() {
        return reservaRepository.countByEstado("Pendiente");
    }
}
