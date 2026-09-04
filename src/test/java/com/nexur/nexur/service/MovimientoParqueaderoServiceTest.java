package com.nexur.nexur.service;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.EstadoMovimientoParqueadero;
import com.nexur.nexur.model.EstadoParqueadero;
import com.nexur.nexur.model.MovimientoParqueadero;
import com.nexur.nexur.model.Parqueadero;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.model.TipoVehiculo;
import com.nexur.nexur.model.Vehiculo;
import com.nexur.nexur.repository.MovimientoParqueaderoRepository;
import com.nexur.nexur.repository.ParqueaderoRepository;
import com.nexur.nexur.repository.VehiculoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovimientoParqueaderoServiceTest {
    @Mock private MovimientoParqueaderoRepository movimientoRepository;
    @Mock private VehiculoRepository vehiculoRepository;
    @Mock private ParqueaderoRepository parqueaderoRepository;

    @Test
    void registraIngresoValidoYSalidaLiberaEspacio() {
        Vehiculo vehiculo = vehiculo();
        Parqueadero parqueadero = parqueadero(EstadoParqueadero.DISPONIBLE);
        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo));
        when(parqueaderoRepository.findById(2L)).thenReturn(Optional.of(parqueadero));
        when(movimientoRepository.findByVehiculoIdAndEstado(1L, EstadoMovimientoParqueadero.DENTRO))
                .thenReturn(Optional.empty());
        when(parqueaderoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(movimientoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MovimientoParqueaderoService service = service();
        MovimientoParqueadero movimiento = service.registrarIngreso(1L, 2L);
        assertEquals(EstadoParqueadero.OCUPADO, parqueadero.getEstado());
        assertEquals(EstadoMovimientoParqueadero.DENTRO, movimiento.getEstado());
        when(movimientoRepository.findById(3L)).thenReturn(Optional.of(movimiento));
        service.registrarSalida(3L);
    }

    @Test
    void rechazaIngresoPorEstadoAsignacionTipoYDuplicidad() {
        Vehiculo vehiculo = vehiculo();
        Parqueadero parqueadero = parqueadero(EstadoParqueadero.MANTENIMIENTO);
        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo));
        when(parqueaderoRepository.findById(2L)).thenReturn(Optional.of(parqueadero));
        when(movimientoRepository.findByVehiculoIdAndEstado(1L, EstadoMovimientoParqueadero.DENTRO))
                .thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service().registrarIngreso(1L, 2L));

        parqueadero.setEstado(EstadoParqueadero.DISPONIBLE);
        parqueadero.setTipo(TipoVehiculo.MOTO);
        assertThrows(IllegalArgumentException.class, () -> service().registrarIngreso(1L, 2L));
        when(movimientoRepository.findByVehiculoIdAndEstado(1L, EstadoMovimientoParqueadero.DENTRO))
                .thenReturn(Optional.of(new MovimientoParqueadero()));
        assertThrows(IllegalArgumentException.class, () -> service().registrarIngreso(1L, 2L));
    }

    @Test
    void rechazaIngresoDeOtroVehiculoYApartamento() {
        Vehiculo vehiculo = vehiculo();
        Parqueadero parqueadero = parqueadero(EstadoParqueadero.ASIGNADO);
        Vehiculo otro = vehiculo();
        otro.setId(9L);
        parqueadero.setVehiculo(otro);
        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo));
        when(parqueaderoRepository.findById(2L)).thenReturn(Optional.of(parqueadero));
        when(movimientoRepository.findByVehiculoIdAndEstado(1L, EstadoMovimientoParqueadero.DENTRO))
                .thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service().registrarIngreso(1L, 2L));
        parqueadero.setVehiculo(null);
        Apartamento otroApartamento = new Apartamento();
        otroApartamento.setId(99L);
        parqueadero.setApartamento(otroApartamento);
        assertThrows(IllegalArgumentException.class, () -> service().registrarIngreso(1L, 2L));
    }

    @Test
    void registraSalidaConservaParqueaderoAsignadoYRechazaSalidaRepetida() {
        MovimientoParqueadero movimiento = new MovimientoParqueadero();
        movimiento.setEstado(EstadoMovimientoParqueadero.DENTRO);
        Parqueadero parqueadero = parqueadero(EstadoParqueadero.OCUPADO);
        Apartamento apartamento = new Apartamento();
        apartamento.setId(10L);
        parqueadero.setApartamento(apartamento);
        movimiento.setParqueadero(parqueadero);
        when(movimientoRepository.findById(3L)).thenReturn(Optional.of(movimiento));
        service().registrarSalida(3L);
        assertEquals(EstadoParqueadero.ASIGNADO, parqueadero.getEstado());
        assertNotNull(movimiento.getFechaHoraSalida());
        movimiento.setEstado(EstadoMovimientoParqueadero.SALIO);
        assertThrows(IllegalArgumentException.class, () -> service().registrarSalida(3L));
    }

    @Test
    void filtraMovimientosYExponeContadores() {
        Vehiculo vehiculo = vehiculo();
        Parqueadero parqueadero = parqueadero(EstadoParqueadero.OCUPADO);
        MovimientoParqueadero movimiento = new MovimientoParqueadero();
        movimiento.setVehiculo(vehiculo);
        movimiento.setParqueadero(parqueadero);
        movimiento.setEstado(EstadoMovimientoParqueadero.DENTRO);
        movimiento.setFechaHoraIngreso(LocalDateTime.of(2026, 8, 20, 10, 0));
        when(movimientoRepository.findAllByOrderByFechaHoraIngresoDesc()).thenReturn(List.of(movimiento));
        when(movimientoRepository.countByEstado(EstadoMovimientoParqueadero.DENTRO)).thenReturn(1L);
        when(parqueaderoRepository.countByEstado(EstadoParqueadero.OCUPADO)).thenReturn(2L);
        assertEquals(1, service().filtrar("abc", TipoVehiculo.CARRO, "P-1",
                EstadoMovimientoParqueadero.DENTRO, LocalDate.of(2026, 8, 20)).size());
        assertEquals(1L, service().contarVehiculosDentro());
        assertEquals(2L, service().contar(EstadoParqueadero.OCUPADO));
        verify(movimientoRepository).findAllByOrderByFechaHoraIngresoDesc();
    }

    private MovimientoParqueaderoService service() {
        return new MovimientoParqueaderoService(movimientoRepository, vehiculoRepository, parqueaderoRepository);
    }

    private Vehiculo vehiculo() {
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setId(1L);
        vehiculo.setPlaca("ABC123");
        vehiculo.setTipo(TipoVehiculo.CARRO);
        Residente residente = new Residente();
        Apartamento apartamento = new Apartamento();
        apartamento.setId(10L);
        residente.setApartamento(apartamento);
        vehiculo.setResidente(residente);
        return vehiculo;
    }

    private Parqueadero parqueadero(EstadoParqueadero estado) {
        Parqueadero parqueadero = new Parqueadero();
        parqueadero.setId(2L);
        parqueadero.setNumero("P-1");
        parqueadero.setTipo(TipoVehiculo.CARRO);
        parqueadero.setEstado(estado);
        return parqueadero;
    }
}
