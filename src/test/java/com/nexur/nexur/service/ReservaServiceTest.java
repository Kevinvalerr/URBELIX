package com.nexur.nexur.service;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.Reserva;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.model.enums.EstadoReserva;
import com.nexur.nexur.model.enums.TipoEspacio;
import com.nexur.nexur.repository.ApartamentoRepository;
import com.nexur.nexur.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock private ReservaRepository reservaRepository;
    @Mock private ApartamentoRepository apartamentoRepository;

    private ReservaService reservaService;
    private Apartamento apartamento;
    private Residente residente;

    @BeforeEach
    void setUp() {
        reservaService = new ReservaService(reservaRepository, apartamentoRepository);
        apartamento = new Apartamento();
        apartamento.setId(1L);
        residente = new Residente();
        residente.setId(2L);
        residente.setApartamento(apartamento);
    }

    @Test
    void residenteSoloPuedeReservarSuApartamento() {
        Apartamento otroApartamento = new Apartamento();
        otroApartamento.setId(9L);
        Reserva reserva = reservaBase();
        reserva.setResidente(residente);
        when(apartamentoRepository.findById(9L)).thenReturn(Optional.of(otroApartamento));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> reservaService.guardar(reserva, 9L));

        assertEquals("El apartamento no pertenece al residente que solicita la reserva",
                exception.getMessage());
    }

    @Test
    void rechazaReservaSinArea() {
        Reserva reserva = reservaBase();
        reserva.setTipoEspacio(null);

        assertThrows(IllegalArgumentException.class, () -> reservaService.guardar(reserva, 1L));
    }

    @Test
    void creaReservaValidaParaElApartamentoDelResidente() {
        Reserva reserva = reservaBase();
        reserva.setResidente(residente);
        when(apartamentoRepository.findById(1L)).thenReturn(Optional.of(apartamento));
        when(reservaRepository.findByTipoEspacioAndEstadoInAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
                eq(TipoEspacio.SALON_SOCIAL), any(), any(), any())).thenReturn(List.of());
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reserva guardada = reservaService.guardar(reserva, 1L);

        assertEquals(apartamento, guardada.getApartamento());
        assertEquals(EstadoReserva.PENDIENTE, guardada.getEstado());
    }

    @Test
    void noPermiteAprobarReservaYaProcesada() {
        Reserva reserva = reservaBase();
        reserva.setEstado(EstadoReserva.RECHAZADA);
        when(reservaRepository.findById(4L)).thenReturn(Optional.of(reserva));

        assertThrows(IllegalArgumentException.class,
                () -> reservaService.aprobarReserva(4L, "Aprobación tardía"));
    }

    private Reserva reservaBase() {
        Reserva reserva = new Reserva();
        reserva.setTipoEspacio(TipoEspacio.SALON_SOCIAL);
        reserva.setFechaInicio(LocalDateTime.now().plusDays(2));
        reserva.setFechaFin(LocalDateTime.now().plusDays(2).plusHours(2));
        return reserva;
    }
}
