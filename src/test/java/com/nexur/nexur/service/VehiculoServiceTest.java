package com.nexur.nexur.service;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.Parqueadero;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.model.TipoVehiculo;
import com.nexur.nexur.model.Vehiculo;
import com.nexur.nexur.repository.MovimientoParqueaderoRepository;
import com.nexur.nexur.repository.ParqueaderoRepository;
import com.nexur.nexur.repository.ResidenteRepository;
import com.nexur.nexur.repository.VehiculoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehiculoServiceTest {

    @Mock private VehiculoRepository vehiculoRepository;
    @Mock private ResidenteRepository residenteRepository;
    @Mock private ParqueaderoRepository parqueaderoRepository;
    @Mock private MovimientoParqueaderoRepository movimientoRepository;

    @Test
    void rechazaVehiculoSinPlacaAntesDePersistir() {
        VehiculoService service = new VehiculoService(vehiculoRepository, residenteRepository,
                parqueaderoRepository, movimientoRepository);

        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setTipo(TipoVehiculo.CARRO);

        assertThrows(IllegalArgumentException.class,
                () -> service.guardar(vehiculo, 1L, null));
    }

    @Test
    void residentePuedeRegistrarSuVehiculo() {
        Residente residente = new Residente();
        residente.setId(7L);
        when(residenteRepository.findByUsuarioEmail("residente@example.com"))
                .thenReturn(Optional.of(residente));
        when(vehiculoRepository.existsByPlacaIgnoreCase("ABC123")).thenReturn(false);
        when(vehiculoRepository.save(any(Vehiculo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setPlaca(" abc123 ");
        vehiculo.setTipo(TipoVehiculo.CARRO);

        VehiculoService service = new VehiculoService(vehiculoRepository, residenteRepository,
                parqueaderoRepository, movimientoRepository);
        Vehiculo guardado = service.guardarParaResidente(vehiculo, "residente@example.com");

        assertEquals("ABC123", guardado.getPlaca());
        assertEquals(residente, guardado.getResidente());
    }

    @Test
    void noPermiteModificarVehiculoConIngresoActivo() {
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setId(3L);
        vehiculo.setPlaca("ABC123");
        vehiculo.setTipo(TipoVehiculo.CARRO);

        when(movimientoRepository.findByVehiculoIdAndEstado(3L,
                com.nexur.nexur.model.EstadoMovimientoParqueadero.DENTRO))
                .thenReturn(Optional.of(new com.nexur.nexur.model.MovimientoParqueadero()));

        VehiculoService service = new VehiculoService(vehiculoRepository, residenteRepository,
                parqueaderoRepository, movimientoRepository);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.guardar(vehiculo, 1L, null));

        assertEquals("No se puede modificar un vehículo mientras está dentro del parqueadero",
                exception.getMessage());
    }

    @Test
    void noPermiteIngresoEnParqueaderoDeOtroApartamento() {
        Apartamento apartamentoVehiculo = new Apartamento();
        apartamentoVehiculo.setId(1L);
        Apartamento apartamentoParqueadero = new Apartamento();
        apartamentoParqueadero.setId(2L);

        Residente residente = new Residente();
        residente.setApartamento(apartamentoVehiculo);
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setId(3L);
        vehiculo.setTipo(TipoVehiculo.CARRO);
        vehiculo.setResidente(residente);

        Parqueadero parqueadero = new Parqueadero();
        parqueadero.setId(4L);
        parqueadero.setTipo(TipoVehiculo.CARRO);
        parqueadero.setEstado(com.nexur.nexur.model.EstadoParqueadero.ASIGNADO);
        parqueadero.setApartamento(apartamentoParqueadero);

        when(vehiculoRepository.findById(3L)).thenReturn(Optional.of(vehiculo));
        when(parqueaderoRepository.findById(4L)).thenReturn(Optional.of(parqueadero));
        when(movimientoRepository.findByVehiculoIdAndEstado(3L,
                com.nexur.nexur.model.EstadoMovimientoParqueadero.DENTRO))
                .thenReturn(Optional.empty());

        VehiculoService service = new VehiculoService(vehiculoRepository, residenteRepository,
                parqueaderoRepository, movimientoRepository);

        MovimientoParqueaderoService movimientoService = new MovimientoParqueaderoService(
                movimientoRepository, vehiculoRepository, parqueaderoRepository);
        assertThrows(IllegalArgumentException.class, () -> movimientoService.registrarIngreso(3L, 4L));
    }
}
