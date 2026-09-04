package com.urbelix.urbelix.service;

import com.urbelix.urbelix.model.Pago;
import com.urbelix.urbelix.model.Reserva;
import com.urbelix.urbelix.model.ReporteRegistro;
import com.urbelix.urbelix.model.Visitante;
import com.urbelix.urbelix.model.Incidencia;
import com.urbelix.urbelix.repository.PagoRepository;
import com.urbelix.urbelix.repository.ReservaRepository;
import com.urbelix.urbelix.repository.VisitanteRepository;
import com.urbelix.urbelix.repository.IncidenciaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReporteService {

    private final PagoRepository pagoRepository;
    private final ReservaRepository reservaRepository;
    private final VisitanteRepository visitanteRepository;
    private final IncidenciaRepository incidenciaRepository;

    public ReporteService(PagoRepository pagoRepository,
                          ReservaRepository reservaRepository,
                          VisitanteRepository visitanteRepository,
                          IncidenciaRepository incidenciaRepository) {
        this.pagoRepository = pagoRepository;
        this.reservaRepository = reservaRepository;
        this.visitanteRepository = visitanteRepository;
        this.incidenciaRepository = incidenciaRepository;
    }

    public List<ReporteRegistro> filtrarRegistros(String tipo, LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null) {
            fechaInicio = LocalDate.now().minusMonths(1);
        }
        if (fechaFin == null) {
            fechaFin = LocalDate.now();
        }
        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(LocalTime.MAX);

        List<ReporteRegistro> registros = new ArrayList<>();
          

        if (tipo == null || tipo.isBlank() || "TODOS".equalsIgnoreCase(tipo)) {
            registros.addAll(mapPagos(pagoRepository.findByFechaBetween(fechaInicio, fechaFin)));
            registros.addAll(mapReservas(reservaRepository.findByFechaInicioBetween(inicio, fin)));
            registros.addAll(mapVisitantes(visitanteRepository.findByFechaEntradaBetween(inicio, fin)));
            registros.addAll(mapIncidencias(incidenciaRepository.findAll().stream().filter(i -> !i.getFechaCreacion().isBefore(inicio) && !i.getFechaCreacion().isAfter(fin)).toList()));
        } else if ("PAGOS".equalsIgnoreCase(tipo)) {
            registros.addAll(mapPagos(pagoRepository.findByFechaBetween(fechaInicio, fechaFin)));
        } else if ("RESERVAS".equalsIgnoreCase(tipo)) {
            registros.addAll(mapReservas(reservaRepository.findByFechaInicioBetween(inicio, fin)));
        } else if ("VISITANTES".equalsIgnoreCase(tipo)) {
            registros.addAll(mapVisitantes(visitanteRepository.findByFechaEntradaBetween(inicio, fin)));
        } else if ("INCIDENCIAS".equalsIgnoreCase(tipo)) {
            registros.addAll(mapIncidencias(incidenciaRepository.findAll().stream().filter(i -> !i.getFechaCreacion().isBefore(inicio) && !i.getFechaCreacion().isAfter(fin)).toList()));
        }

        registros.sort((a, b) -> b.getFechaHora().compareTo(a.getFechaHora()));
        return registros;
    }

    private List<ReporteRegistro> mapIncidencias(List<Incidencia> incidencias) {
        return incidencias.stream().map(incidencia -> new ReporteRegistro(
                "Incidencia", "Incidencia #" + incidencia.getId(),
                incidencia.getResidente() == null ? "N/A" : incidencia.getResidente().getNombre(),
                incidencia.getTitulo() + " | " + incidencia.getEstado() + " | " + incidencia.getPrioridad(),
                incidencia.getFechaCreacion())).collect(Collectors.toList());
    }

    private List<ReporteRegistro> mapPagos(List<Pago> pagos) {
        return pagos.stream()
                .map(pago -> new ReporteRegistro(
                        "Pago",
                        "Pago #" + pago.getId(),
                       pago.getResidente() != null ? pago.getResidente().getNombre() : "—",
                        "Pago de " + pago.getMonto() + " por apto " + (pago.getApartamento() != null ? pago.getApartamento().getNumero() : "N/A"),
                        pago.getCreadoEn()
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
