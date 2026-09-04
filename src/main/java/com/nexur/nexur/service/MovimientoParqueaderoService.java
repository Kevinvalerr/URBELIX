package com.nexur.nexur.service;

import com.nexur.nexur.model.*;
import com.nexur.nexur.repository.MovimientoParqueaderoRepository;
import com.nexur.nexur.repository.ParqueaderoRepository;
import com.nexur.nexur.repository.VehiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class MovimientoParqueaderoService {
    private final MovimientoParqueaderoRepository movimientoRepository;
    private final VehiculoRepository vehiculoRepository;
    private final ParqueaderoRepository parqueaderoRepository;

    public MovimientoParqueaderoService(MovimientoParqueaderoRepository movimientoRepository,
                                        VehiculoRepository vehiculoRepository,
                                        ParqueaderoRepository parqueaderoRepository) {
        this.movimientoRepository = movimientoRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.parqueaderoRepository = parqueaderoRepository;
    }

    public List<MovimientoParqueadero> listar() { return movimientoRepository.findAllByOrderByFechaHoraIngresoDesc(); }
    public List<MovimientoParqueadero> listarActivos() { return movimientoRepository.findByEstadoOrderByFechaHoraIngresoDesc(EstadoMovimientoParqueadero.DENTRO); }

        public List<MovimientoParqueadero> filtrar(String placa, TipoVehiculo tipo, String parqueadero,
                               EstadoMovimientoParqueadero estado, LocalDate fecha) {
        String placaFiltro = placa == null ? "" : placa.trim().toLowerCase(Locale.ROOT);
        String parqueaderoFiltro = parqueadero == null ? "" : parqueadero.trim().toLowerCase(Locale.ROOT);
        return listar().stream()
            .filter(movimiento -> placaFiltro.isBlank()
                || movimiento.getVehiculo().getPlaca().toLowerCase(Locale.ROOT).contains(placaFiltro))
            .filter(movimiento -> tipo == null || movimiento.getVehiculo().getTipo() == tipo)
            .filter(movimiento -> parqueaderoFiltro.isBlank()
                || movimiento.getParqueadero().getNumero().toLowerCase(Locale.ROOT).contains(parqueaderoFiltro))
            .filter(movimiento -> estado == null || movimiento.getEstado() == estado)
            .filter(movimiento -> fecha == null || movimiento.getFechaHoraIngreso().toLocalDate().equals(fecha))
            .collect(Collectors.toList());
        }

    @Transactional
    public MovimientoParqueadero registrarIngreso(Long vehiculoId, Long parqueaderoId) {
        Vehiculo vehiculo = vehiculoRepository.findById(vehiculoId)
                .orElseThrow(() -> new IllegalArgumentException("El vehículo no existe"));
        Parqueadero parqueadero = parqueaderoRepository.findById(parqueaderoId)
                .orElseThrow(() -> new IllegalArgumentException("El parqueadero no existe"));
        if (movimientoRepository.findByVehiculoIdAndEstado(vehiculoId, EstadoMovimientoParqueadero.DENTRO).isPresent()) {
            throw new IllegalArgumentException("El vehículo ya tiene un ingreso activo");
        }
        if (parqueadero.getEstado() != EstadoParqueadero.DISPONIBLE
                && parqueadero.getEstado() != EstadoParqueadero.ASIGNADO) {
            throw new IllegalArgumentException("El parqueadero no está disponible");
        }
        if (parqueadero.getVehiculo() != null && !parqueadero.getVehiculo().getId().equals(vehiculoId)) {
            throw new IllegalArgumentException("El parqueadero está asignado a otro vehículo");
        }
        if (parqueadero.getApartamento() != null
                && (vehiculo.getResidente() == null
                || vehiculo.getResidente().getApartamento() == null
                || !parqueadero.getApartamento().getId().equals(
                        vehiculo.getResidente().getApartamento().getId()))) {
            throw new IllegalArgumentException("El vehículo no pertenece al apartamento del parqueadero");
        }
        if (parqueadero.getTipo() != vehiculo.getTipo()) {
            throw new IllegalArgumentException("El tipo de vehículo no coincide con el parqueadero");
        }
        MovimientoParqueadero movimiento = new MovimientoParqueadero();
        movimiento.setVehiculo(vehiculo);
        movimiento.setParqueadero(parqueadero);
        movimiento.setFechaHoraIngreso(LocalDateTime.now());
        movimiento.setEstado(EstadoMovimientoParqueadero.DENTRO);
        parqueadero.setVehiculo(vehiculo);
        parqueadero.setEstado(EstadoParqueadero.OCUPADO);
        parqueaderoRepository.save(parqueadero);
        return movimientoRepository.save(movimiento);
    }

    @Transactional
    public void registrarSalida(Long movimientoId) {
        MovimientoParqueadero movimiento = movimientoRepository.findById(movimientoId)
                .orElseThrow(() -> new IllegalArgumentException("Movimiento no encontrado"));
        if (movimiento.getEstado() != EstadoMovimientoParqueadero.DENTRO) {
            throw new IllegalArgumentException("El vehículo ya tiene registrada su salida");
        }
        movimiento.setFechaHoraSalida(LocalDateTime.now());
        movimiento.setEstado(EstadoMovimientoParqueadero.SALIO);
        Parqueadero parqueadero = movimiento.getParqueadero();
        if (parqueadero.getApartamento() == null) {
            parqueadero.setVehiculo(null);
            parqueadero.setEstado(EstadoParqueadero.DISPONIBLE);
        } else {
            parqueadero.setEstado(EstadoParqueadero.ASIGNADO);
        }
        parqueaderoRepository.save(parqueadero);
        movimientoRepository.save(movimiento);
    }

    public long contar(EstadoParqueadero estado) { return parqueaderoRepository.countByEstado(estado); }
    public long contarVehiculosDentro() { return movimientoRepository.countByEstado(EstadoMovimientoParqueadero.DENTRO); }
}
