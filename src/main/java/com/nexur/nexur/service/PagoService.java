package com.nexur.nexur.service;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.Pago;
import com.nexur.nexur.repository.ApartamentoRepository;
import com.nexur.nexur.repository.PagoRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;
 import com.nexur.nexur.repository.ResidenteRepository;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.model.enums.EstadoPago;
import com.nexur.nexur.model.enums.MetodoPago;
import com.nexur.nexur.model.enums.TipoPago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class PagoService {

    private final PagoRepository pagoRepository;
    private final ApartamentoRepository apartamentoRepository;
    private final ResidenteRepository residenteRepository;

   public PagoService(PagoRepository pagoRepository, 
                   ApartamentoRepository apartamentoRepository,
                   ResidenteRepository residenteRepository) {
    this.pagoRepository = pagoRepository;
    this.apartamentoRepository = apartamentoRepository;
    this.residenteRepository = residenteRepository;
}

    public List<Pago> listarPagos() {
        return pagoRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    public List<Pago> obtenerUltimosPagos() {
        return pagoRepository.findTop4ByOrderByIdDesc();
    }

    public Pago guardar(Pago pago, Long apartamentoId) {


System.out.println("DEBUG PAGO:");
System.out.println("Residente objeto: " + pago.getResidente());
System.out.println("Residente ID: " + 
    (pago.getResidente() != null ? pago.getResidente().getId() : "NULL"));
   
    Apartamento apartamento = apartamentoRepository.findById(apartamentoId)
            .orElseThrow(() -> new RuntimeException("Apartamento no encontrado"));
    pago.setApartamento(apartamento);


    
    if (pago.getResidente() != null && pago.getResidente().getId() != null) {
        Residente residente = residenteRepository.findById(pago.getResidente().getId())
                .orElseThrow(() -> new RuntimeException("Residente no encontrado"));
        pago.setResidente(residente);
    } else {
        throw new RuntimeException("Debe seleccionar un residente");
    }
    
    // 🔹 lógica de estado automática
if (pago.getFecha() != null && pago.getFechaVencimiento() != null) {

    if (pago.getFecha().isAfter(pago.getFechaVencimiento())) {
        pago.setEstadoPago(EstadoPago.VENCIDO);
    } else {
        pago.setEstadoPago(EstadoPago.PAGADO);
    }

} else {
    pago.setEstadoPago(EstadoPago.PENDIENTE);
}
    
    
    return pagoRepository.save(pago);
}


public long contarPagos() {
    return pagoRepository.count();

}

public Pago buscarPorId(Long id) {
    return pagoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Pago no encontrado"));
}

public void marcarComoPagado(Long id) {
    Pago pago = buscarPorId(id);
    pago.setEstadoPago(EstadoPago.PAGADO);
    pagoRepository.save(pago);
}

public List<Pago> listarPagosPorUsuario(String username) {
    return pagoRepository.findByResidenteUsuarioEmail(username);
}

public List<Pago> obtenerPagosVencidos() {
    return pagoRepository.findByEstadoPago(EstadoPago.VENCIDO);
}

public List<Pago> obtenerPagosVencidosPorUsuario(String email) {
    return pagoRepository.findByResidenteUsuarioEmailAndEstadoPago(email, EstadoPago.VENCIDO);
}

public void generarPagosAdministracion() {

    List<Residente> residentes = residenteRepository.findAll();

    for (Residente residente : residentes) {

        Pago pago = new Pago();
        pago.setResidente(residente);
        pago.setApartamento(residente.getApartamento());
        pago.setMonto(new BigDecimal("300000")); // valor ejemplo
        pago.setTipoPago(TipoPago.ADMINISTRACION);
        pago.setMetodo(MetodoPago.TRANSFERENCIA);
        pago.setFecha(LocalDate.now());
        pago.setFechaVencimiento(LocalDate.now().plusDays(30));
        pago.setEstadoPago(EstadoPago.PENDIENTE);

        pagoRepository.save(pago);
    }
}
}
