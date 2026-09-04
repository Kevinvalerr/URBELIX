package com.nexur.nexur.service;

import com.nexur.nexur.model.Parqueadero;
import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.EstadoMovimientoParqueadero;
import com.nexur.nexur.model.EstadoParqueadero;
import com.nexur.nexur.model.TipoVehiculo;
import com.nexur.nexur.model.Vehiculo;
import com.nexur.nexur.repository.ApartamentoRepository;
import com.nexur.nexur.repository.MovimientoParqueaderoRepository;
import com.nexur.nexur.repository.ParqueaderoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ParqueaderoServiceTest {

    @Mock private ParqueaderoRepository parqueaderoRepository;
    @Mock private ApartamentoRepository apartamentoRepository;
    @Mock private MovimientoParqueaderoRepository movimientoRepository;

    @Test
    void rechazaParqueaderoSinNumeroAntesDePersistir() {
        ParqueaderoService service = new ParqueaderoService(parqueaderoRepository,
                apartamentoRepository, movimientoRepository);

        assertThrows(IllegalArgumentException.class, () -> service.guardar(new Parqueadero(), null));
    }

    @Test
    void noPermiteDesasignarVehiculoAlEditarParqueaderoDesdeFormulario() {
        Parqueadero existente = new Parqueadero();
        existente.setId(10L);
        existente.setNumero("P-10");
        existente.setTipo(TipoVehiculo.CARRO);
        existente.setEstado(EstadoParqueadero.ASIGNADO);

        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setId(20L);
        vehiculo.setTipo(TipoVehiculo.CARRO);
        existente.setVehiculo(vehiculo);

        Parqueadero formulario = new Parqueadero();
        formulario.setId(10L);
        formulario.setNumero("P-10");
        formulario.setTipo(TipoVehiculo.CARRO);
        formulario.setEstado(EstadoParqueadero.DISPONIBLE);

        when(parqueaderoRepository.existsByNumeroIgnoreCaseAndIdNot("P-10", 10L)).thenReturn(false);
        when(parqueaderoRepository.findById(10L)).thenReturn(Optional.of(existente));

        ParqueaderoService service = new ParqueaderoService(parqueaderoRepository,
                apartamentoRepository, movimientoRepository);

        assertThrows(IllegalArgumentException.class, () -> service.guardar(formulario, null));
    }

    @Test
    void conservaApartamentoCuandoParqueaderoOcupadoEsEditado() {
        Apartamento apartamento = new Apartamento();
        apartamento.setId(1L);

        Parqueadero existente = new Parqueadero();
        existente.setId(10L);
        existente.setNumero("P-10");
        existente.setTipo(TipoVehiculo.CARRO);
        existente.setEstado(EstadoParqueadero.ASIGNADO);
        existente.setApartamento(apartamento);

        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setId(20L);
        vehiculo.setTipo(TipoVehiculo.CARRO);
        existente.setVehiculo(vehiculo);

        Parqueadero formulario = new Parqueadero();
        formulario.setId(10L);
        formulario.setNumero("P-10");
        formulario.setTipo(TipoVehiculo.CARRO);
        formulario.setEstado(EstadoParqueadero.OCUPADO);

        when(parqueaderoRepository.existsByNumeroIgnoreCaseAndIdNot("P-10", 10L)).thenReturn(false);
        when(parqueaderoRepository.findById(10L)).thenReturn(Optional.of(existente));
        when(movimientoRepository.findByVehiculoIdAndEstado(20L, EstadoMovimientoParqueadero.DENTRO))
                .thenReturn(Optional.empty());
        when(parqueaderoRepository.save(any(Parqueadero.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ParqueaderoService service = new ParqueaderoService(parqueaderoRepository,
                apartamentoRepository, movimientoRepository);

        Parqueadero guardado = service.guardar(formulario, null);

        org.junit.jupiter.api.Assertions.assertSame(apartamento, guardado.getApartamento());
        org.junit.jupiter.api.Assertions.assertSame(vehiculo, guardado.getVehiculo());
    }

    @Test
    void creaParqueaderoConValoresPorDefectoYListaYBusca() {
        Parqueadero formulario = new Parqueadero();
        formulario.setNumero(" p-01 ");
        when(parqueaderoRepository.existsByNumero("P-01")).thenReturn(false);
        when(parqueaderoRepository.save(any(Parqueadero.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Parqueadero guardado = service().guardar(formulario, null);
        assertEquals("P-01", guardado.getNumero());
        assertEquals(EstadoParqueadero.DISPONIBLE, guardado.getEstado());
        assertEquals(TipoVehiculo.CARRO, guardado.getTipo());
        when(parqueaderoRepository.findAllByOrderByNumeroAsc()).thenReturn(java.util.List.of(guardado));
        when(parqueaderoRepository.findByApartamentoIdOrderByNumeroAsc(1L)).thenReturn(java.util.List.of(guardado));
        assertEquals(1, service().listarTodos().size());
        assertEquals(1, service().listarPorApartamento(1L).size());
    }

    @Test
    void rechazaDuplicadoAsignadoSinApartamentoYOcupadoSinVehiculo() {
        Parqueadero duplicado = new Parqueadero();
        duplicado.setNumero("P-10");
        when(parqueaderoRepository.existsByNumero("P-10")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service().guardar(duplicado, null));

        Parqueadero asignado = new Parqueadero();
        asignado.setNumero("P-11");
        asignado.setEstado(EstadoParqueadero.ASIGNADO);
        when(parqueaderoRepository.existsByNumero("P-11")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> service().guardar(asignado, null));

        Parqueadero ocupado = new Parqueadero();
        ocupado.setNumero("P-12");
        ocupado.setEstado(EstadoParqueadero.OCUPADO);
        when(parqueaderoRepository.existsByNumero("P-12")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> service().guardar(ocupado, null));
    }

    @Test
    void validaApartamentoTipoYVehiculoDentro() {
        Parqueadero formulario = new Parqueadero();
        formulario.setId(20L);
        formulario.setNumero("P-20");
        formulario.setEstado(EstadoParqueadero.ASIGNADO);
        when(parqueaderoRepository.existsByNumeroIgnoreCaseAndIdNot("P-20", 20L)).thenReturn(false);
        when(parqueaderoRepository.findById(20L)).thenReturn(Optional.of(formulario));
        when(apartamentoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service().guardar(formulario, 99L));

        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setId(21L);
        vehiculo.setTipo(TipoVehiculo.MOTO);
        formulario.setEstado(EstadoParqueadero.DISPONIBLE);
        formulario.setVehiculo(vehiculo);
        formulario.setTipo(TipoVehiculo.CARRO);
        when(movimientoRepository.findByVehiculoIdAndEstado(21L, EstadoMovimientoParqueadero.DENTRO))
                .thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service().guardar(formulario, null));
        formulario.setTipo(TipoVehiculo.MOTO);
        formulario.setEstado(EstadoParqueadero.ASIGNADO);
        Apartamento apartamento = new Apartamento();
        apartamento.setId(10L);
        formulario.setApartamento(apartamento);
        when(apartamentoRepository.findById(10L)).thenReturn(Optional.of(apartamento));
        when(movimientoRepository.findByVehiculoIdAndEstado(21L, EstadoMovimientoParqueadero.DENTRO))
                .thenReturn(Optional.of(new com.nexur.nexur.model.MovimientoParqueadero()));
        assertThrows(IllegalArgumentException.class, () -> service().guardar(formulario, 10L));
    }

    @Test
    void eliminaSoloParqueaderoSinRelaciones() {
        Parqueadero parqueadero = new Parqueadero();
        when(parqueaderoRepository.findById(30L)).thenReturn(Optional.of(parqueadero));
        when(movimientoRepository.existsByParqueaderoId(30L)).thenReturn(false);
        service().eliminar(30L);
        verify(parqueaderoRepository).deleteById(30L);
        when(parqueaderoRepository.findById(31L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service().buscarPorId(31L));
    }

    private ParqueaderoService service() {
        return new ParqueaderoService(parqueaderoRepository, apartamentoRepository, movimientoRepository);
    }
}
