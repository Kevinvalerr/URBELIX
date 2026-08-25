package com.nexur.nexur.service;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.repository.ApartamentoRepository;
import com.nexur.nexur.repository.PagoRepository;
import com.nexur.nexur.repository.ReservaRepository;

import org.springframework.stereotype.Service;
import java.util.List;

@Service

public class ApartamentoService {
    
    private final ApartamentoRepository apartamentoRepository;
    private final PagoRepository pagoRepository;
    private final ReservaRepository reservaRepository;

    public ApartamentoService(ApartamentoRepository apartamentoRepository,
                              PagoRepository pagoRepository,
                              ReservaRepository reservaRepository){
        this.apartamentoRepository = apartamentoRepository;
        this.pagoRepository = pagoRepository;
        this.reservaRepository = reservaRepository;
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
        long pagosAsociados = pagoRepository.countByApartamentoId(id);
        long reservasAsociadas = reservaRepository.countByApartamentoId(id);

        if (pagosAsociados > 0 || reservasAsociadas > 0) {
            String mensaje = "No se puede eliminar el apartamento porque tiene ";
            if (pagosAsociados > 0) {
                mensaje += pagosAsociados + " pago" + (pagosAsociados > 1 ? "s" : "") + " asociado" + (pagosAsociados > 1 ? "s" : "");
            }
            if (pagosAsociados > 0 && reservasAsociadas > 0) {
                mensaje += " y ";
            }
            if (reservasAsociadas > 0) {
                mensaje += reservasAsociadas + " reserva" + (reservasAsociadas > 1 ? "s" : "") + " asociada" + (reservasAsociadas > 1 ? "s" : "");
            }
            mensaje += ".";
            throw new RuntimeException(mensaje);
        }
        apartamentoRepository.deleteById(id);
    }

}
