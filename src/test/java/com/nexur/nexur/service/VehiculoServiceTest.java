package com.nexur.nexur.service;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.EstadoParqueadero;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

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

    @Test
    void rechazaPlacaTipoResidenteYDuplicado() {
        VehiculoService service = service();
        assertThrows(IllegalArgumentException.class, () -> service.guardar(null, 1L, null));
        Vehiculo sinPlaca = new Vehiculo();
        assertThrows(IllegalArgumentException.class, () -> service.guardar(sinPlaca, 1L, null));
        Vehiculo sinTipo = vehiculo("abc123", null);
        assertThrows(IllegalArgumentException.class, () -> service.guardar(sinTipo, 1L, null));
        Vehiculo duplicado = vehiculo("abc123", TipoVehiculo.CARRO);
        when(vehiculoRepository.existsByPlacaIgnoreCase("ABC123")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.guardar(duplicado, 1L, null));
    }

    @Test
    void rechazaResidenteAusenteYResidenteNoEncontrado() {
        Vehiculo vehiculo = vehiculo("ABC123", TipoVehiculo.CARRO);
        assertThrows(IllegalArgumentException.class, () -> service().guardar(vehiculo, null, null));
        when(residenteRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> service().guardar(vehiculo("ABC123", TipoVehiculo.CARRO), 2L, null));
    }

    @Test
    void rechazaParqueaderoInexistenteOcupadoYTipoIncompatible() {
        when(residenteRepository.findById(1L)).thenReturn(Optional.of(residente()));
        when(vehiculoRepository.save(any(Vehiculo.class))).thenAnswer(invocation -> {
            Vehiculo guardado = invocation.getArgument(0);
            guardado.setId(30L);
            return guardado;
        });
        when(parqueaderoRepository.findByVehiculoId(30L)).thenReturn(Optional.empty());
        when(parqueaderoRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> service().guardar(vehiculo("ABC124", TipoVehiculo.CARRO), 1L, 9L));

        Parqueadero ocupado = parqueadero(9L, TipoVehiculo.CARRO, EstadoParqueadero.OCUPADO);
        Vehiculo otro = vehiculo("XYZ999", TipoVehiculo.CARRO);
        otro.setId(99L);
        ocupado.setVehiculo(otro);
        when(parqueaderoRepository.findById(9L)).thenReturn(Optional.of(ocupado));
        assertThrows(IllegalArgumentException.class,
                () -> service().guardar(vehiculo("ABC125", TipoVehiculo.CARRO), 1L, 9L));

        Parqueadero moto = parqueadero(9L, TipoVehiculo.MOTO, EstadoParqueadero.DISPONIBLE);
        when(parqueaderoRepository.findById(9L)).thenReturn(Optional.of(moto));
        assertThrows(IllegalArgumentException.class,
                () -> service().guardar(vehiculo("ABC126", TipoVehiculo.CARRO), 1L, 9L));
    }

    @Test
    void rechazaParqueaderoNoDisponibleYApartamentoDiferente() {
        when(residenteRepository.findById(1L)).thenReturn(Optional.of(residente()));
        when(vehiculoRepository.save(any(Vehiculo.class))).thenAnswer(invocation -> {
            Vehiculo guardado = invocation.getArgument(0);
            guardado.setId(31L);
            return guardado;
        });
        when(parqueaderoRepository.findByVehiculoId(31L)).thenReturn(Optional.empty());
        Parqueadero parqueadero = parqueadero(9L, TipoVehiculo.CARRO, EstadoParqueadero.MANTENIMIENTO);
        when(parqueaderoRepository.findById(9L)).thenReturn(Optional.of(parqueadero));
        assertThrows(IllegalArgumentException.class,
                () -> service().guardar(vehiculo("ABC127", TipoVehiculo.CARRO), 1L, 9L));

        parqueadero.setEstado(EstadoParqueadero.DISPONIBLE);
        Apartamento otro = new Apartamento();
        otro.setId(99L);
        parqueadero.setApartamento(otro);
        assertThrows(IllegalArgumentException.class,
                () -> service().guardar(vehiculo("ABC128", TipoVehiculo.CARRO), 1L, 9L));
    }

    @Test
    void eliminaVehiculoYLiberaParqueadero() {
        Vehiculo vehiculo = vehiculo("ABC123", TipoVehiculo.CARRO);
        vehiculo.setId(7L);
        Parqueadero parqueadero = parqueadero(2L, TipoVehiculo.CARRO, EstadoParqueadero.OCUPADO);
        when(vehiculoRepository.findById(7L)).thenReturn(Optional.of(vehiculo));
        when(movimientoRepository.existsByVehiculoId(7L)).thenReturn(false);
        when(parqueaderoRepository.findByVehiculoId(7L)).thenReturn(Optional.of(parqueadero));

        service().eliminar(7L);

        assertEquals(EstadoParqueadero.DISPONIBLE, parqueadero.getEstado());
        verify(vehiculoRepository).delete(vehiculo);
    }

    @Test
    void rechazaEliminarConHistorialYConsultarVehiculoAjeno() {
        Vehiculo vehiculo = vehiculo("ABC123", TipoVehiculo.CARRO);
        vehiculo.setId(7L);
        when(vehiculoRepository.findById(7L)).thenReturn(Optional.of(vehiculo));
        when(movimientoRepository.existsByVehiculoId(7L)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service().eliminar(7L));

        Residente otro = residente();
        otro.setId(8L);
        vehiculo.setResidente(otro);
        when(residenteRepository.findByUsuarioEmail("residente@example.com"))
                .thenReturn(Optional.of(residente()));
        assertThrows(IllegalArgumentException.class,
                () -> service().buscarParaResidente(7L, "residente@example.com"));
    }

    @Test
    void cubreConsultasYListadosCuandoExisteOFaltaElRegistro() {
        Vehiculo vehiculo = vehiculo("ABC123", TipoVehiculo.CARRO);
        when(vehiculoRepository.findAllByOrderByPlacaAsc()).thenReturn(List.of(vehiculo));
        when(vehiculoRepository.findById(2L)).thenReturn(Optional.of(vehiculo));
        when(vehiculoRepository.findById(99L)).thenReturn(Optional.empty());
        when(residenteRepository.findByUsuarioEmail("residente@example.com"))
                .thenReturn(Optional.of(residente()));
        when(residenteRepository.findByUsuarioEmail("ausente@example.com"))
                .thenReturn(Optional.empty());

        VehiculoService service = service();

        assertEquals(1, service.listar().size());
        assertEquals(vehiculo, service.buscar(2L));
        assertEquals(residente().getId(), service.buscarResidente("residente@example.com").getId());
        assertThrows(IllegalArgumentException.class, () -> service.buscar(99L));
        assertThrows(IllegalArgumentException.class,
                () -> service.buscarResidente("ausente@example.com"));
    }

    @Test
    void asignaParqueaderoDisponibleSinApartamento() {
        when(residenteRepository.findById(1L)).thenReturn(Optional.of(residente()));
        when(vehiculoRepository.save(any(Vehiculo.class))).thenAnswer(invocation -> {
            Vehiculo guardado = invocation.getArgument(0);
            guardado.setId(40L);
            return guardado;
        });
        when(parqueaderoRepository.findByVehiculoId(40L)).thenReturn(Optional.empty());
        Parqueadero parqueadero = parqueadero(9L, TipoVehiculo.CARRO, EstadoParqueadero.DISPONIBLE);
        when(parqueaderoRepository.findById(9L)).thenReturn(Optional.of(parqueadero));

        Vehiculo guardado = service().guardar(vehiculo("ABC130", TipoVehiculo.CARRO), 1L, 9L);

        assertEquals(1L, guardado.getResidente().getId());
        assertEquals(EstadoParqueadero.ASIGNADO, parqueadero.getEstado());
        assertEquals(guardado, parqueadero.getVehiculo());
    }

    @Test
    void liberaParqueaderoAnteriorConApartamentoAsignado() {
        Residente residente = residente();
        Vehiculo vehiculo = vehiculo("ABC131", TipoVehiculo.CARRO);
        vehiculo.setId(41L);
        Parqueadero anterior = parqueadero(8L, TipoVehiculo.CARRO, EstadoParqueadero.OCUPADO);
        anterior.setApartamento(residente.getApartamento());
        when(residenteRepository.findById(1L)).thenReturn(Optional.of(residente));
        when(vehiculoRepository.existsByPlacaIgnoreCaseAndIdNot("ABC131", 41L)).thenReturn(false);
        when(vehiculoRepository.save(any(Vehiculo.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(parqueaderoRepository.findByVehiculoId(41L)).thenReturn(Optional.of(anterior));

        service().guardar(vehiculo, 1L, null);

        assertEquals(null, anterior.getVehiculo());
        assertEquals(EstadoParqueadero.ASIGNADO, anterior.getEstado());
        verify(parqueaderoRepository).save(anterior);
    }

    @Test
    void actualizaVehiculoPropioYConservaAsignacionExistente() {
        Residente residente = residente();
        Vehiculo existente = vehiculo("ABC132", TipoVehiculo.CARRO);
        existente.setId(42L);
        existente.setResidente(residente);
        Vehiculo formulario = vehiculo(" abc133 ", TipoVehiculo.MOTO);
        formulario.setId(42L);
        formulario.setMarca(" Marca ");
        when(residenteRepository.findByUsuarioEmail("residente@example.com"))
                .thenReturn(Optional.of(residente));
        when(vehiculoRepository.findById(42L)).thenReturn(Optional.of(existente));
        when(movimientoRepository.findByVehiculoIdAndEstado(42L,
                com.nexur.nexur.model.EstadoMovimientoParqueadero.DENTRO))
                .thenReturn(Optional.empty());
        when(vehiculoRepository.existsByPlacaIgnoreCaseAndIdNot("ABC133", 42L)).thenReturn(false);
        when(vehiculoRepository.save(any(Vehiculo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Vehiculo guardado = service().guardarParaResidente(formulario, "residente@example.com");

        assertEquals("ABC133", guardado.getPlaca());
        assertEquals(TipoVehiculo.MOTO, guardado.getTipo());
        assertEquals("Marca", guardado.getMarca());
    }

    @Test
    void permiteReasignarElMismoParqueaderoAlMismoVehiculo() {
        Residente residente = residente();
        when(residenteRepository.findById(1L)).thenReturn(Optional.of(residente));
        when(vehiculoRepository.save(any(Vehiculo.class))).thenAnswer(invocation -> {
            Vehiculo guardado = invocation.getArgument(0);
            guardado.setId(43L);
            return guardado;
        });
        when(parqueaderoRepository.findByVehiculoId(43L)).thenReturn(Optional.empty());
        Parqueadero parqueadero = parqueadero(9L, TipoVehiculo.CARRO, EstadoParqueadero.ASIGNADO);
        Vehiculo anterior = vehiculo("OLD000", TipoVehiculo.CARRO);
        anterior.setId(43L);
        parqueadero.setVehiculo(anterior);
        when(parqueaderoRepository.findById(9L)).thenReturn(Optional.of(parqueadero));

        Vehiculo guardado = service().guardar(vehiculo("ABC134", TipoVehiculo.CARRO), 1L, 9L);

        assertEquals(guardado, parqueadero.getVehiculo());
        assertEquals(EstadoParqueadero.ASIGNADO, parqueadero.getEstado());
    }

    private VehiculoService service() {
        return new VehiculoService(vehiculoRepository, residenteRepository,
                parqueaderoRepository, movimientoRepository);
    }

    private Vehiculo vehiculo(String placa, TipoVehiculo tipo) {
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setPlaca(placa);
        vehiculo.setTipo(tipo);
        return vehiculo;
    }

    private Residente residente() {
        Residente residente = new Residente();
        residente.setId(1L);
        Apartamento apartamento = new Apartamento();
        apartamento.setId(10L);
        residente.setApartamento(apartamento);
        return residente;
    }

    private Parqueadero parqueadero(Long id, TipoVehiculo tipo, EstadoParqueadero estado) {
        Parqueadero parqueadero = new Parqueadero();
        parqueadero.setId(id);
        parqueadero.setTipo(tipo);
        parqueadero.setEstado(estado);
        return parqueadero;
    }
}
