package com.nexur.nexur.service;

import com.nexur.nexur.model.Pago;
import com.nexur.nexur.model.Reserva;
import com.nexur.nexur.model.ReporteRegistro;
import com.nexur.nexur.model.Visitante;
import com.nexur.nexur.repository.PagoRepository;
import com.nexur.nexur.repository.ReservaRepository;
import com.nexur.nexur.repository.VisitanteRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReporteService {

    private final PagoRepository pagoRepository;
    private final ReservaRepository reservaRepository;
    private final VisitanteRepository visitanteRepository;

    public ReporteService(PagoRepository pagoRepository,
                          ReservaRepository reservaRepository,
                          VisitanteRepository visitanteRepository) {
        this.pagoRepository = pagoRepository;
        this.reservaRepository = reservaRepository;
        this.visitanteRepository = visitanteRepository;
    }

    public List<ReporteRegistro> filtrarRegistros(String tipo, LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null) {
            fechaInicio = LocalDate.now().minusMonths(1);
        }
        if (fechaFin == null) {
            fechaFin = LocalDate.now();
        }
        if (fechaInicio.isAfter(fechaFin)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final");
        }

        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(LocalTime.MAX);

        List<ReporteRegistro> registros = new ArrayList<>();
        String tipoNormalizado = tipo == null ? "TODOS" : tipo.trim().toUpperCase();

        switch (tipoNormalizado) {
            case "TODOS" -> {
                registros.addAll(mapPagos(pagoRepository.findByFechaBetween(fechaInicio, fechaFin)));
                registros.addAll(mapReservas(reservaRepository.findByFechaInicioBetween(inicio, fin)));
                registros.addAll(mapVisitantes(visitanteRepository.findByFechaEntradaBetween(inicio, fin)));
            }
            case "PAGOS" -> registros.addAll(mapPagos(
                    pagoRepository.findByFechaBetween(fechaInicio, fechaFin)));
            case "RESERVAS" -> registros.addAll(mapReservas(
                    reservaRepository.findByFechaInicioBetween(inicio, fin)));
            case "VISITANTES" -> registros.addAll(mapVisitantes(
                    visitanteRepository.findByFechaEntradaBetween(inicio, fin)));
            default -> throw new IllegalArgumentException("Tipo de reporte no válido: " + tipo);
        }

        registros.sort(Comparator.comparing(ReporteRegistro::getFechaHora,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return registros;
    }

    private List<ReporteRegistro> mapPagos(List<Pago> pagos) {
        return pagos.stream()
                .map(pago -> new ReporteRegistro(
                        "Pago",
                        "Pago #" + pago.getId(),
                       pago.getResidente() != null ? pago.getResidente().getNombre() : "—",
                        "Pago de " + pago.getMonto() + " por apto " + (pago.getApartamento() != null ? pago.getApartamento().getNumero() : "N/A"),
                        pago.getFecha() == null ? pago.getCreadoEn() : pago.getFecha().atStartOfDay()
                ))
                .collect(Collectors.toList());
    }

    private List<ReporteRegistro> mapReservas(List<Reserva> reservas) {
        return reservas.stream()
                .map(reserva -> new ReporteRegistro(
                        "Reserva",
                        "Reserva #" + reserva.getId(),
                        reserva.getResidente() != null ? reserva.getResidente().getNombre() : "N/A",
                        "Reserva de " + reserva.getTipoEspacio() + " para apto " + (reserva.getApartamento() != null ? reserva.getApartamento().getNumero() : "N/A"),
                        reserva.getCreadoEn()
                ))
                .collect(Collectors.toList());
    }

    private List<ReporteRegistro> mapVisitantes(List<Visitante> visitantes) {
        return visitantes.stream()
                .map(visitante -> new ReporteRegistro(
                        "Visita",
                        "Visita #" + visitante.getId(),
                        visitante.getNombre(),
                        "Entrada registrada en apto " + (visitante.getApartamento() != null ? visitante.getApartamento().getNumero() : "N/A"),
                        visitante.getFechaEntrada()
                ))
                .collect(Collectors.toList());
    }
}
