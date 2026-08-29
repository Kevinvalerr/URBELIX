package com.nexur.nexur.service;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.Pago;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.model.enums.EstadoPago;
import com.nexur.nexur.model.enums.MetodoPago;
import com.nexur.nexur.repository.ApartamentoRepository;
import com.nexur.nexur.repository.PagoRepository;
import com.nexur.nexur.repository.ResidenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private ApartamentoRepository apartamentoRepository;
    @Mock
    private ResidenteRepository residenteRepository;

    private PagoService pagoService;
    private Residente residente;
    private Apartamento apartamento;

    @BeforeEach
    void setUp() {
        pagoService = new PagoService(pagoRepository, apartamentoRepository, residenteRepository,
                new BigDecimal("300000"));
        residente = new Residente();
        residente.setId(1L);
        apartamento = new Apartamento();
        apartamento.setId(10L);
        residente.setApartamento(apartamento);
    }

    @Test
    void conservaEstadoPendienteAlCrearCuotaConVencimientoFuturo() {
        when(residenteRepository.findById(1L)).thenReturn(Optional.of(residente));
        when(apartamentoRepository.findById(10L)).thenReturn(Optional.of(apartamento));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pago pago = new Pago();
        pago.setMonto(new BigDecimal("300000"));
        pago.setFecha(LocalDate.now());
        pago.setFechaVencimiento(LocalDate.now().plusDays(30));
        pago.setEstadoPago(EstadoPago.PENDIENTE);

        Pago guardado = pagoService.guardar(pago, 1L, 10L);

        assertEquals(EstadoPago.PENDIENTE, guardado.getEstadoPago());
    }

    @Test
    void rechazaApartamentoQueNoPerteneceAlResidente() {
        when(residenteRepository.findById(1L)).thenReturn(Optional.of(residente));
        Apartamento otroApartamento = new Apartamento();
        otroApartamento.setId(20L);
        when(apartamentoRepository.findById(20L)).thenReturn(Optional.of(otroApartamento));

        Pago pago = new Pago();
        pago.setMonto(new BigDecimal("100000"));
        pago.setFecha(LocalDate.now());
        pago.setFechaVencimiento(LocalDate.now().plusDays(30));

        assertThrows(IllegalArgumentException.class, () -> pagoService.guardar(pago, 1L, 20L));
    }

    @Test
    void generaReferenciaPseSoloParaElPropietario() {
        Usuario usuario = new Usuario();
        usuario.setEmail("residente@example.com");
        residente.setUsuario(usuario);
        Pago pago = new Pago();
        pago.setMonto(new BigDecimal("100000"));
        pago.setEstadoPago(EstadoPago.PENDIENTE);
        pago.setMetodo(MetodoPago.PSE);
        pago.setResidente(residente);
        when(pagoRepository.findById(3L)).thenReturn(Optional.of(pago));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pago preparado = pagoService.iniciarPagoPse(3L, "residente@example.com");

        assertTrue(preparado.getReferenciaPago().startsWith("PSE-"));
        assertThrows(IllegalArgumentException.class,
                () -> pagoService.iniciarPagoPse(3L, "otro@example.com"));
    }

    @Test
    void noPermiteConfirmarPseDesdeElFlujoManual() {
        Pago pago = new Pago();
        pago.setMonto(new BigDecimal("100000"));
        pago.setEstadoPago(EstadoPago.PENDIENTE);
        pago.setMetodo(MetodoPago.PSE);
        when(pagoRepository.findById(4L)).thenReturn(Optional.of(pago));

        assertThrows(IllegalArgumentException.class, () -> pagoService.marcarComoPagado(4L));
    }

    @Test
    void permiteRegularizarUnPagoVencido() {
        Pago pago = new Pago();
        pago.setEstadoPago(EstadoPago.VENCIDO);
        pago.setMetodo(MetodoPago.TRANSFERENCIA);
        when(pagoRepository.findById(5L)).thenReturn(Optional.of(pago));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(invocation -> invocation.getArgument(0));

        pagoService.marcarComoPagado(5L);

        assertEquals(EstadoPago.PAGADO, pago.getEstadoPago());
        assertNotNull(pago.getFechaPago());
    }

    @Test
    void confirmaTransferenciaYRegistraLaFechaEfectiva() {
        Pago pago = new Pago();
        pago.setEstadoPago(EstadoPago.PENDIENTE);
        pago.setMetodo(MetodoPago.TRANSFERENCIA);
        when(pagoRepository.findById(6L)).thenReturn(Optional.of(pago));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(invocation -> invocation.getArgument(0));

        pagoService.marcarComoPagado(6L);

        assertEquals(EstadoPago.PAGADO, pago.getEstadoPago());
        assertNotNull(pago.getFechaPago());
    }

    @Test
    void conservaLaFechaDeEmisionComoRespaldoParaPagosHistoricos() {
        Pago pago = new Pago();
        pago.setEstadoPago(EstadoPago.PAGADO);
        pago.setFecha(LocalDate.of(2026, 8, 20));
        when(pagoRepository.findByEstadoPagoAndFechaVencimientoBefore(any(), any()))
                .thenReturn(java.util.List.of());
        when(pagoRepository.findByEstadoPagoAndFechaPagoIsNull(EstadoPago.PAGADO))
                .thenReturn(java.util.List.of(pago));

        pagoService.listarPagos();

        assertEquals(LocalDate.of(2026, 8, 20), pago.getFechaPago());
    }

    @Test
    void noPermiteConfirmarTarjetaDeFormaManual() {
        Pago pago = new Pago();
        pago.setEstadoPago(EstadoPago.PENDIENTE);
        pago.setMetodo(MetodoPago.TARJETA);
        when(pagoRepository.findById(7L)).thenReturn(Optional.of(pago));

        assertThrows(IllegalArgumentException.class, () -> pagoService.marcarComoPagado(7L));
    }
}
