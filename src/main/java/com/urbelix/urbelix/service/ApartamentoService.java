package com.urbelix.urbelix.service;

import com.urbelix.urbelix.model.Apartamento;
import com.urbelix.urbelix.repository.ApartamentoRepository;
import com.urbelix.urbelix.repository.PagoRepository;
import com.urbelix.urbelix.repository.ReservaRepository;

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

    public boolean existePorNumero(String numero) {
        return apartamentoRepository.existsByNumero(numero);
    }

    public String generarSiguienteNumero() {
        int siguiente = listarApartamentos().stream()
                .map(Apartamento::getNumero)
                .filter(numero -> numero != null && numero.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0) + 1;

        String numero = String.valueOf(siguiente);
        while (existePorNumero(numero)) {
            numero = String.valueOf(++siguiente);
        }
        return numero;
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
