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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
}
