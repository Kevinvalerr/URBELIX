package com.urbelix.urbelix.service;

import com.urbelix.urbelix.model.Apartamento;
import com.urbelix.urbelix.model.Pago;
import com.urbelix.urbelix.repository.ApartamentoRepository;
import com.urbelix.urbelix.repository.PagoRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 import com.urbelix.urbelix.repository.ResidenteRepository;
import com.urbelix.urbelix.model.Residente;
import com.urbelix.urbelix.model.enums.EstadoPago;
import com.urbelix.urbelix.model.enums.MetodoPago;
import com.urbelix.urbelix.model.enums.TipoPago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.time.format.DateTimeFormatter;

@Service
public class PagoService {

    private static final Logger log = LoggerFactory.getLogger(PagoService.class);

    private final PagoRepository pagoRepository;
    private final ApartamentoRepository apartamentoRepository;
    private final ResidenteRepository residenteRepository;
    private final AuditoriaService auditoriaService;

   public PagoService(PagoRepository pagoRepository, 
                   ApartamentoRepository apartamentoRepository,
                   ResidenteRepository residenteRepository,
                   AuditoriaService auditoriaService) {
    this.pagoRepository = pagoRepository;
    this.apartamentoRepository = apartamentoRepository;
    this.residenteRepository = residenteRepository;
    this.auditoriaService = auditoriaService;
}

    public List<Pago> listarPagos() {
        return pagoRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    public List<Pago> obtenerUltimosPagos() {
        return pagoRepository.findTop4ByOrderByIdDesc();
    }

    @Transactional
    public Pago guardar(Pago pago, Long residenteId, Long apartamentoId) {

        if (residenteId == null) {
            if (pago.getResidente() != null && pago.getResidente().getId() != null) {
                residenteId = pago.getResidente().getId();
            } else {
                throw new RuntimeException("Debe seleccionar un residente");
            }
        }
        Residente residente = residenteRepository.findById(residenteId)
                .orElseThrow(() -> new RuntimeException("Residente no encontrado"));
        pago.setResidente(residente);

        if (apartamentoId == null) {
            if (pago.getApartamento() != null && pago.getApartamento().getId() != null) {
                apartamentoId = pago.getApartamento().getId();
            } else if (residente.getApartamento() != null) {
                apartamentoId = residente.getApartamento().getId();
            } else {
                throw new RuntimeException("Debe seleccionar un apartamento");
            }
        }

        Apartamento apartamento = apartamentoRepository.findById(apartamentoId)
                .orElseThrow(() -> new RuntimeException("Apartamento no encontrado"));
        pago.setApartamento(apartamento);

        if (pago.getEstadoPago() == null) {
            pago.setEstadoPago(EstadoPago.PENDIENTE);
        }
        if (pago.getEstadoPago() == EstadoPago.PENDIENTE
                && pago.getFechaVencimiento() != null
                && LocalDate.now().isAfter(pago.getFechaVencimiento())) {
            pago.setEstadoPago(EstadoPago.VENCIDO);
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

    @Transactional
    public void marcarComoPagado(Long id) {
    Pago pago = buscarPorId(id);
    if (pago.getEstadoPago() != EstadoPago.PENDIENTE) {
        throw new IllegalStateException("Solo se puede aprobar un pago pendiente");
    }
    pago.setEstadoPago(EstadoPago.PAGADO);
    pago.setReferenciaPago(generarReferencia(pago));
    pagoRepository.save(pago);
    auditoriaService.registrar("CONFIRMAR_PAGO", "PAGOS", "Pago", id,
        "EXITO", "Pago confirmado en simulación académica");
}

@Transactional
public void registrarRechazoSimulado(Long id) {
    Pago pago = buscarPorId(id);
    if (pago.getEstadoPago() != EstadoPago.PENDIENTE) {
        throw new IllegalStateException("Solo se puede rechazar un pago pendiente");
    }
    auditoriaService.registrar("RECHAZAR_PAGO", "PAGOS", "Pago", id,
        "EXITO", "Pago rechazado en simulación; permanece pendiente");
}

private String generarReferencia(Pago pago) {
    return "URB-" + pago.getFecha().format(DateTimeFormatter.BASIC_ISO_DATE)
        + "-" + String.format("%06d", pago.getId());
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

@Transactional
public void generarPagosAdministracion() {

    List<Residente> residentes = residenteRepository.findAll();

    if (residentes.isEmpty()) {
        throw new RuntimeException("No hay residentes registrados en el sistema");
    }

    for (Residente residente : residentes) {
        if (residente.getApartamento() == null) {
            log.warn("Residente id={} sin apartamento; pago omitido", residente.getId());
            continue;
        }

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
