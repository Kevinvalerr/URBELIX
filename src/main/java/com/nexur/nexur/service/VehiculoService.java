package com.nexur.nexur.service;

import com.nexur.nexur.model.EstadoParqueadero;
import com.nexur.nexur.model.Parqueadero;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.model.Vehiculo;
import com.nexur.nexur.repository.ParqueaderoRepository;
import com.nexur.nexur.repository.ResidenteRepository;
import com.nexur.nexur.repository.VehiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.util.List;

@Service
public class VehiculoService {
    private final VehiculoRepository vehiculoRepository;
    private final ResidenteRepository residenteRepository;
    private final ParqueaderoRepository parqueaderoRepository;
    private final com.nexur.nexur.repository.MovimientoParqueaderoRepository movimientoRepository;

    public VehiculoService(VehiculoRepository vehiculoRepository,
                           ResidenteRepository residenteRepository,
                           ParqueaderoRepository parqueaderoRepository,
                           com.nexur.nexur.repository.MovimientoParqueaderoRepository movimientoRepository) {
        this.vehiculoRepository = vehiculoRepository;
        this.residenteRepository = residenteRepository;
        this.parqueaderoRepository = parqueaderoRepository;
        this.movimientoRepository = movimientoRepository;
    }

    public List<Vehiculo> listar() { return vehiculoRepository.findAllByOrderByPlacaAsc(); }

    public Vehiculo buscar(Long id) {
        return vehiculoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehículo no encontrado"));
    }

    public Residente buscarResidente(String email) {
        return residenteRepository.findByUsuarioEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("El usuario no tiene un perfil de residente"));
    }

    public Vehiculo buscarParaResidente(Long id, String email) {
        Vehiculo vehiculo = buscar(id);
        Residente residente = buscarResidente(email);
        if (vehiculo.getResidente() == null || !residente.getId().equals(vehiculo.getResidente().getId())) {
            throw new IllegalArgumentException("No puede consultar ese vehículo");
        }
        return vehiculo;
    }

    @Transactional
    public Vehiculo guardar(Vehiculo vehiculo, Long residenteId, Long parqueaderoId) {
        prepararDatosVehiculo(vehiculo);
        validarSinIngresoActivo(vehiculo.getId());
        String placa = vehiculo.getPlaca();
        vehiculo.setPlaca(placa);
        if ((vehiculo.getId() == null && vehiculoRepository.existsByPlacaIgnoreCase(placa))
                || (vehiculo.getId() != null && vehiculoRepository.existsByPlacaIgnoreCaseAndIdNot(placa, vehiculo.getId()))) {
            throw new IllegalArgumentException("Ya existe un vehículo con esa placa");
        }
        if (residenteId == null) {
            throw new IllegalArgumentException("Debe seleccionar un residente");
        }
        Residente residente = residenteRepository.findById(residenteId)
                .orElseThrow(() -> new IllegalArgumentException("Residente no encontrado"));
        vehiculo.setResidente(residente);
        Vehiculo guardado = vehiculoRepository.save(vehiculo);

        Parqueadero anterior = parqueaderoRepository.findByVehiculoId(guardado.getId()).orElse(null);
        if (anterior != null && (parqueaderoId == null || !anterior.getId().equals(parqueaderoId))) {
            anterior.setVehiculo(null);
            if (anterior.getEstado() == EstadoParqueadero.ASIGNADO || anterior.getEstado() == EstadoParqueadero.OCUPADO) {
                anterior.setEstado(anterior.getApartamento() == null ? EstadoParqueadero.DISPONIBLE : EstadoParqueadero.ASIGNADO);
            }
            parqueaderoRepository.save(anterior);
        }
        if (parqueaderoId != null) {
            Parqueadero parqueadero = parqueaderoRepository.findById(parqueaderoId)
                    .orElseThrow(() -> new IllegalArgumentException("Parqueadero no encontrado"));
            if (parqueadero.getVehiculo() != null && !parqueadero.getVehiculo().getId().equals(guardado.getId())) {
                throw new IllegalArgumentException("El parqueadero ya tiene un vehículo asignado");
            }
            if (parqueadero.getTipo() != vehiculo.getTipo()) {
                throw new IllegalArgumentException("El tipo de vehículo no coincide con el parqueadero");
            }
            if (parqueadero.getApartamento() != null && residente.getApartamento() != null
                    && !parqueadero.getApartamento().getId().equals(residente.getApartamento().getId())) {
                throw new IllegalArgumentException("El vehículo debe pertenecer al residente del apartamento asignado");
            }
            if (parqueadero.getEstado() == EstadoParqueadero.MANTENIMIENTO
                    || parqueadero.getEstado() == EstadoParqueadero.RESERVADO
                    || parqueadero.getEstado() == EstadoParqueadero.OCUPADO) {
                throw new IllegalArgumentException("El parqueadero no está disponible para asignación");
            }
            parqueadero.setVehiculo(guardado);
            parqueadero.setEstado(EstadoParqueadero.ASIGNADO);
            parqueaderoRepository.save(parqueadero);
        }
        return guardado;
    }

    @Transactional
    public Vehiculo guardarParaResidente(Vehiculo formulario, String email) {
        prepararDatosVehiculo(formulario);
        Residente residente = buscarResidente(email);
        Vehiculo vehiculo;
        if (formulario.getId() == null) {
            vehiculo = formulario;
        } else {
            vehiculo = buscar(formulario.getId());
            if (vehiculo.getResidente() == null || !residente.getId().equals(vehiculo.getResidente().getId())) {
                throw new IllegalArgumentException("No puede modificar ese vehículo");
            }
            validarSinIngresoActivo(vehiculo.getId());
            vehiculo.setPlaca(formulario.getPlaca());
            vehiculo.setTipo(formulario.getTipo());
            vehiculo.setMarca(formulario.getMarca());
            vehiculo.setModelo(formulario.getModelo());
            vehiculo.setColor(formulario.getColor());
        }
        if ((vehiculo.getId() == null && vehiculoRepository.existsByPlacaIgnoreCase(vehiculo.getPlaca()))
                || (vehiculo.getId() != null && vehiculoRepository.existsByPlacaIgnoreCaseAndIdNot(
                        vehiculo.getPlaca(), vehiculo.getId()))) {
            throw new IllegalArgumentException("Ya existe un vehículo con esa placa");
        }
        vehiculo.setResidente(residente);
        return vehiculoRepository.save(vehiculo);
    }

    public List<Vehiculo> listarPorResidente(Long residenteId) {
        return vehiculoRepository.findByResidenteIdOrderByPlacaAsc(residenteId);
    }

    private void prepararDatosVehiculo(Vehiculo vehiculo) {
        if (vehiculo == null || !StringUtils.hasText(vehiculo.getPlaca())) {
            throw new IllegalArgumentException("La placa es obligatoria");
        }
        if (vehiculo.getTipo() == null) {
            throw new IllegalArgumentException("Debe seleccionar el tipo de vehículo");
        }
        vehiculo.setPlaca(vehiculo.getPlaca().trim().toUpperCase());
        vehiculo.setMarca(normalizarOpcional(vehiculo.getMarca()));
        vehiculo.setModelo(normalizarOpcional(vehiculo.getModelo()));
        vehiculo.setColor(normalizarOpcional(vehiculo.getColor()));
    }

    private String normalizarOpcional(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }

    private void validarSinIngresoActivo(Long vehiculoId) {
        if (vehiculoId != null && movimientoRepository.findByVehiculoIdAndEstado(
                vehiculoId, com.nexur.nexur.model.EstadoMovimientoParqueadero.DENTRO).isPresent()) {
            throw new IllegalArgumentException("No se puede modificar un vehículo mientras está dentro del parqueadero");
        }
    }

    @Transactional
    public void eliminar(Long id) {
        Vehiculo vehiculo = buscar(id);
        if (movimientoRepository.existsByVehiculoId(id)) {
            throw new IllegalArgumentException("No se puede eliminar un vehículo con movimientos en el historial");
        }
        Parqueadero parqueadero = parqueaderoRepository.findByVehiculoId(id).orElse(null);
        if (parqueadero != null) {
            parqueadero.setVehiculo(null);
            parqueadero.setEstado(parqueadero.getApartamento() == null ? EstadoParqueadero.DISPONIBLE : EstadoParqueadero.ASIGNADO);
            parqueaderoRepository.save(parqueadero);
        }
        vehiculoRepository.delete(vehiculo);
    }
}
