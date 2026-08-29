package com.nexur.nexur.service;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.Pago;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.model.enums.EstadoPago;
import com.nexur.nexur.model.enums.MetodoPago;
import com.nexur.nexur.model.enums.TipoPago;
import com.nexur.nexur.repository.ApartamentoRepository;
import com.nexur.nexur.repository.PagoRepository;
import com.nexur.nexur.repository.ResidenteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PagoService {

    private static final Logger log = LoggerFactory.getLogger(PagoService.class);

    private final PagoRepository pagoRepository;
    private final ApartamentoRepository apartamentoRepository;
    private final ResidenteRepository residenteRepository;
    private final BigDecimal montoAdministracion;

    public PagoService(PagoRepository pagoRepository,
                       ApartamentoRepository apartamentoRepository,
                       ResidenteRepository residenteRepository,
                       @Value("${app.administration.amount:300000}") BigDecimal montoAdministracion) {
        this.pagoRepository = pagoRepository;
        this.apartamentoRepository = apartamentoRepository;
        this.residenteRepository = residenteRepository;
        this.montoAdministracion = montoAdministracion;
    }

    @Transactional
    public List<Pago> listarPagos() {
        actualizarPagosVencidos();
        return pagoRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @Transactional
    public List<Pago> obtenerUltimosPagos() {
        actualizarPagosVencidos();
        return pagoRepository.findTop4ByOrderByIdDesc();
    }

    @Transactional
    public Pago guardar(Pago pago, Long residenteId, Long apartamentoId) {
        if (residenteId == null && pago.getResidente() != null) {
            residenteId = pago.getResidente().getId();
        }
        if (residenteId == null) {
            throw new IllegalArgumentException("Debe seleccionar un residente");
        }

        Residente residente = residenteRepository.findById(residenteId)
                .orElseThrow(() -> new IllegalArgumentException("Residente no encontrado"));
        pago.setResidente(residente);

        if (apartamentoId == null && pago.getApartamento() != null) {
            apartamentoId = pago.getApartamento().getId();
        }
        if (apartamentoId == null && residente.getApartamento() != null) {
            apartamentoId = residente.getApartamento().getId();
        }
        if (apartamentoId == null) {
            throw new IllegalArgumentException("Debe seleccionar un apartamento");
        }

        Apartamento apartamento = apartamentoRepository.findById(apartamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Apartamento no encontrado"));
        if (residente.getApartamento() == null
                || !residente.getApartamento().getId().equals(apartamento.getId())) {
            throw new IllegalArgumentException("El apartamento no pertenece al residente seleccionado");
        }
        if (pago.getFecha() != null && pago.getFechaVencimiento() != null
                && pago.getFechaVencimiento().isBefore(pago.getFecha())) {
            throw new IllegalArgumentException("La fecha de vencimiento no puede ser anterior a la fecha del pago");
        }

        pago.setApartamento(apartamento);
        if (pago.getEstadoPago() == null) {
            pago.setEstadoPago(EstadoPago.PENDIENTE);
        }
        return pagoRepository.save(pago);
    }

    public long contarPagos() {
        return pagoRepository.count();
    }

    public Pago buscarPorId(Long id) {
        return pagoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));
    }

    @Transactional
    public void marcarComoPagado(Long id) {
        Pago pago = buscarPorId(id);
        if (pago.getMetodo() == MetodoPago.PSE || pago.getMetodo() == MetodoPago.TARJETA) {
            throw new IllegalArgumentException(
                    "Los pagos en línea solo se confirman cuando el proveedor valida la transacción");
        }
        if (pago.getEstadoPago() == EstadoPago.PAGADO) {
            if (pago.getFechaPago() == null) {
                pago.setFechaPago(LocalDate.now());
                pagoRepository.save(pago);
            }
            return;
        }
        if (pago.getEstadoPago() != EstadoPago.PENDIENTE
                && pago.getEstadoPago() != EstadoPago.VENCIDO) {
            throw new IllegalArgumentException("Solo se puede confirmar un pago pendiente o vencido");
        }
        pago.setEstadoPago(EstadoPago.PAGADO);
        pago.setFechaPago(LocalDate.now());
        pagoRepository.save(pago);
    }

    @Transactional
    public Pago iniciarPagoPse(Long id, String email) {
        return iniciarPagoOnline(id, email);
    }

    @Transactional
    public Pago iniciarPagoOnline(Long id, String email) {
        Pago pago = buscarPorId(id);
        if (pago.getResidente() == null || pago.getResidente().getUsuario() == null
                || email == null || !email.equalsIgnoreCase(pago.getResidente().getUsuario().getEmail())) {
            throw new IllegalArgumentException("No puede iniciar este pago");
        }
        if (pago.getMetodo() != MetodoPago.PSE && pago.getMetodo() != MetodoPago.TARJETA) {
            throw new IllegalArgumentException("Este pago no está configurado para un checkout en línea");
        }
        if (pago.getEstadoPago() != EstadoPago.PENDIENTE
                && pago.getEstadoPago() != EstadoPago.VENCIDO) {
            throw new IllegalArgumentException("Solo se puede iniciar un pago pendiente o vencido");
        }
        if (!StringUtils.hasText(pago.getReferenciaPago())) {
            String prefijo = pago.getMetodo() == MetodoPago.TARJETA ? "CARD-" : "PSE-";
            pago.setReferenciaPago(prefijo + UUID.randomUUID());
            pagoRepository.save(pago);
        }
        return pago;
    }

    @Transactional
    public List<Pago> listarPagosPorUsuario(String username) {
        actualizarPagosVencidos();
        return pagoRepository.findByResidenteUsuarioEmail(username);
    }

    @Transactional
    public List<Pago> obtenerPagosVencidos() {
        actualizarPagosVencidos();
        return pagoRepository.findByEstadoPago(EstadoPago.VENCIDO);
    }

    @Transactional
    public List<Pago> obtenerPagosVencidosPorUsuario(String email) {
        actualizarPagosVencidos();
        return pagoRepository.findByResidenteUsuarioEmailAndEstadoPago(email, EstadoPago.VENCIDO);
    }

    private void actualizarPagosVencidos() {
        List<Pago> pendientesVencidos = pagoRepository
                .findByEstadoPagoAndFechaVencimientoBefore(EstadoPago.PENDIENTE, LocalDate.now());
        if (!pendientesVencidos.isEmpty()) {
            pendientesVencidos.forEach(pago -> pago.setEstadoPago(EstadoPago.VENCIDO));
            pagoRepository.saveAll(pendientesVencidos);
        }

        List<Pago> pagadosSinFecha = pagoRepository
                .findByEstadoPagoAndFechaPagoIsNull(EstadoPago.PAGADO);
        if (!pagadosSinFecha.isEmpty()) {
            pagadosSinFecha.forEach(pago -> pago.setFechaPago(
                    pago.getFecha() == null ? LocalDate.now() : pago.getFecha()));
            pagoRepository.saveAll(pagadosSinFecha);
        }
    }

    @Transactional
    public void generarPagosAdministracion() {
        List<Residente> residentes = residenteRepository.findAll();
        if (residentes.isEmpty()) {
            throw new IllegalArgumentException("No hay residentes registrados en el sistema");
        }

        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
        LocalDate finMes = inicioMes.plusMonths(1).minusDays(1);
        int creados = 0;

        for (Residente residente : residentes) {
            if (residente.getApartamento() == null) {
                log.warn("Residente {} no tiene apartamento asignado; se omite", residente.getNombre());
                continue;
            }
            if (pagoRepository.existsByResidenteIdAndTipoPagoAndFechaBetween(
                    residente.getId(), TipoPago.ADMINISTRACION, inicioMes, finMes)) {
                continue;
            }

            Pago pago = new Pago();
            pago.setResidente(residente);
            pago.setApartamento(residente.getApartamento());
            pago.setMonto(montoAdministracion);
            pago.setTipoPago(TipoPago.ADMINISTRACION);
            pago.setMetodo(MetodoPago.TRANSFERENCIA);
            pago.setFecha(LocalDate.now());
            pago.setFechaVencimiento(LocalDate.now().plusDays(30));
            pago.setEstadoPago(EstadoPago.PENDIENTE);
            pagoRepository.save(pago);
            creados++;
        }
        log.info("Generación de administración completada: {} pagos creados", creados);
    }
}
