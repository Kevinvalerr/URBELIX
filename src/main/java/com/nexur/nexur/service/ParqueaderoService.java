package com.nexur.nexur.service;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.EstadoParqueadero;
import com.nexur.nexur.model.Parqueadero;
import com.nexur.nexur.repository.ApartamentoRepository;
import com.nexur.nexur.repository.MovimientoParqueaderoRepository;
import com.nexur.nexur.repository.ParqueaderoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ParqueaderoService {

    private final ParqueaderoRepository parqueaderoRepository;
    private final ApartamentoRepository apartamentoRepository;
    private final MovimientoParqueaderoRepository movimientoRepository;

    public ParqueaderoService(ParqueaderoRepository parqueaderoRepository,
                              ApartamentoRepository apartamentoRepository,
                              MovimientoParqueaderoRepository movimientoRepository) {
        this.parqueaderoRepository = parqueaderoRepository;
        this.apartamentoRepository = apartamentoRepository;
        this.movimientoRepository = movimientoRepository;
    }

    public List<Parqueadero> listarTodos() {
        return parqueaderoRepository.findAllByOrderByNumeroAsc();
    }

    public List<Parqueadero> listarPorApartamento(Long apartamentoId) {
        return parqueaderoRepository.findByApartamentoIdOrderByNumeroAsc(apartamentoId);
    }

    public Parqueadero buscarPorId(Long id) {
        return parqueaderoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Parqueadero no encontrado"));
    }

    @Transactional
    public Parqueadero guardar(Parqueadero parqueadero, Long apartamentoId) {
        if (parqueadero == null || !StringUtils.hasText(parqueadero.getNumero())) {
            throw new IllegalArgumentException("El número del parqueadero es obligatorio");
        }
        String numero = parqueadero.getNumero().trim().toUpperCase();
        boolean numeroDuplicado = parqueadero.getId() == null
                ? parqueaderoRepository.existsByNumero(numero)
                : parqueaderoRepository.existsByNumeroIgnoreCaseAndIdNot(numero, parqueadero.getId());
        if (numeroDuplicado) {
            throw new IllegalArgumentException("Ya existe un parqueadero con ese número");
        }

        Parqueadero destino = parqueadero.getId() == null
                ? parqueadero
                : parqueaderoRepository.findById(parqueadero.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Parqueadero no encontrado"));
        destino.setNumero(numero);
        destino.setZona(StringUtils.hasText(parqueadero.getZona())
                ? parqueadero.getZona().trim() : null);
        destino.setEstado(parqueadero.getEstado() == null
                ? EstadoParqueadero.DISPONIBLE : parqueadero.getEstado());
        destino.setTipo(parqueadero.getTipo() == null
                ? com.nexur.nexur.model.TipoVehiculo.CARRO : parqueadero.getTipo());

        Apartamento apartamento = null;
        if (apartamentoId != null) {
            apartamento = apartamentoRepository.findById(apartamentoId)
                    .orElseThrow(() -> new IllegalArgumentException("Apartamento no encontrado"));
        } else if (destino.getEstado() == EstadoParqueadero.OCUPADO
                && destino.getApartamento() != null) {
            apartamento = destino.getApartamento();
        }

        if (destino.getEstado() == EstadoParqueadero.ASIGNADO && apartamento == null) {
            throw new IllegalArgumentException("Un parqueadero asignado debe tener un apartamento");
        }
        if (destino.getEstado() == EstadoParqueadero.OCUPADO
                && destino.getVehiculo() == null) {
            throw new IllegalArgumentException("Un parqueadero ocupado debe tener un vehículo");
        }
        if (destino.getVehiculo() != null
                && destino.getEstado() != EstadoParqueadero.ASIGNADO
                && destino.getEstado() != EstadoParqueadero.OCUPADO) {
            throw new IllegalArgumentException("Retire el vehículo antes de cambiar el parqueadero a ese estado");
        }

        if (destino.getVehiculo() != null) {
            if (destino.getTipo() != destino.getVehiculo().getTipo()) {
                throw new IllegalArgumentException("El tipo de vehículo no coincide con el parqueadero");
            }
            if (apartamento != null && destino.getVehiculo().getResidente() != null
                    && destino.getVehiculo().getResidente().getApartamento() != null
                    && !apartamento.getId().equals(
                            destino.getVehiculo().getResidente().getApartamento().getId())) {
                throw new IllegalArgumentException("El parqueadero debe pertenecer al apartamento del vehículo");
            }
            if (movimientoRepository.findByVehiculoIdAndEstado(destino.getVehiculo().getId(),
                    com.nexur.nexur.model.EstadoMovimientoParqueadero.DENTRO).isPresent()
                    && destino.getEstado() != EstadoParqueadero.OCUPADO) {
                throw new IllegalArgumentException(
                        "El parqueadero debe permanecer ocupado mientras el vehículo está dentro");
            }
        }

        // Un parqueadero ocupado conserva la asignación residencial para que la salida
        // no lo convierta en un espacio disponible por accidente.
        if (destino.getEstado() != EstadoParqueadero.ASIGNADO
                && destino.getEstado() != EstadoParqueadero.OCUPADO) {
            apartamento = null;
        }

        destino.setApartamento(apartamento);
        return parqueaderoRepository.save(destino);
    }

    @Transactional
    public void eliminar(Long id) {
        Parqueadero parqueadero = buscarPorId(id);
        if (parqueadero.getVehiculo() != null || movimientoRepository.existsByParqueaderoId(id)) {
            throw new IllegalArgumentException("No se puede eliminar un parqueadero con vehículo o movimientos asociados");
        }
        parqueaderoRepository.deleteById(id);
    }
}
