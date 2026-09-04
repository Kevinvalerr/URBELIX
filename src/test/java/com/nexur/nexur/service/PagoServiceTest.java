package com.nexur.nexur.service;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.Pago;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.model.enums.EstadoPago;
import com.nexur.nexur.model.enums.MetodoPago;
import com.nexur.nexur.model.enums.TipoPago;
import com.nexur.nexur.repository.ApartamentoRepository;
import com.nexur.nexur.repository.PagoRepository;
import com.nexur.nexur.repository.ResidenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

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
    void creaObligacionInicialParaResidenteNuevo() {
        when(pagoRepository.save(any(Pago.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pago pago = pagoService.crearObligacionInicial(residente);

        assertEquals(residente, pago.getResidente());
        assertEquals(apartamento, pago.getApartamento());
        assertEquals(new BigDecimal("300000"), pago.getMonto());
        assertEquals(TipoPago.ADMINISTRACION, pago.getTipoPago());
        assertEquals(MetodoPago.TRANSFERENCIA, pago.getMetodo());
        assertEquals(EstadoPago.PENDIENTE, pago.getEstadoPago());
        assertNotNull(pago.getFechaVencimiento());
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

    @Test
    void confirmaPagoSimuladoPendienteYCompletaFechaDePagoExistente() {
        Pago pendiente = new Pago();
        pendiente.setEstadoPago(EstadoPago.PENDIENTE);
        when(pagoRepository.save(any(Pago.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Pago confirmado = pagoService.confirmarPagoSimulado(pendiente);
        assertEquals(EstadoPago.PAGADO, confirmado.getEstadoPago());
        assertNotNull(confirmado.getFechaPago());
        verify(pagoRepository).save(pendiente);

        Pago pagado = new Pago();
        pagado.setEstadoPago(EstadoPago.PAGADO);
        pagoService.confirmarPagoSimulado(pagado);
        assertNotNull(pagado.getFechaPago());
        verify(pagoRepository).save(pagado);
    }

    @Test
    void rechazaConfirmacionSimuladaNulaODeEstadoNoValido() {
        assertThrows(IllegalArgumentException.class, () -> pagoService.confirmarPagoSimulado(null));
        Pago cancelado = new Pago();
        cancelado.setEstadoPago(null);
        assertThrows(IllegalArgumentException.class, () -> pagoService.confirmarPagoSimulado(cancelado));
    }

    @Test
    void registraTrazabilidadDelSandboxYApruebaSoloResultadoValido() {
        Pago pago = new Pago();
        pago.setEstadoPago(EstadoPago.PENDIENTE);
        when(pagoRepository.save(any(Pago.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pago guardado = pagoService.registrarResultadoSimulado(pago, "APPROVED", "SIM-123");

        assertEquals(EstadoPago.PAGADO, guardado.getEstadoPago());
        assertEquals("APPROVED", guardado.getResultadoSimulacion());
        assertEquals("SIM-123", guardado.getTransaccionSimulada());
        assertNotNull(guardado.getSimuladoEn());
        assertNotNull(guardado.getFechaPago());
        verify(pagoRepository).save(pago);
    }

    @Test
    void rechazaTrazabilidadInvalidaOPagoCerrado() {
        Pago pago = new Pago();
        pago.setEstadoPago(EstadoPago.PENDIENTE);

        assertThrows(IllegalArgumentException.class,
                () -> pagoService.registrarResultadoSimulado(pago, "INVALID", "SIM-123"));
        assertThrows(IllegalArgumentException.class,
                () -> pagoService.registrarResultadoSimulado(pago, "APPROVED", " "));

        pago.setEstadoPago(EstadoPago.PAGADO);
        assertThrows(IllegalArgumentException.class,
                () -> pagoService.registrarResultadoSimulado(pago, "APPROVED", "SIM-123"));
        verify(pagoRepository, never()).save(pago);
    }

    @Test
    void noReescribeFechaDePagoYaRegistrada() {
        Pago pagado = new Pago();
        pagado.setEstadoPago(EstadoPago.PAGADO);
        pagado.setFechaPago(LocalDate.of(2026, 8, 15));

        pagoService.confirmarPagoSimulado(pagado);

        assertEquals(LocalDate.of(2026, 8, 15), pagado.getFechaPago());
        verify(pagoRepository, never()).save(pagado);
    }

    @Test
    void guardarUsaResidenteYApartamentoDelPagoYConservaEstado() {
        Pago pago = new Pago();
        pago.setResidente(residente);
        pago.setApartamento(apartamento);
        pago.setEstadoPago(EstadoPago.VENCIDO);
        when(residenteRepository.findById(1L)).thenReturn(Optional.of(residente));
        when(apartamentoRepository.findById(10L)).thenReturn(Optional.of(apartamento));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pago guardado = pagoService.guardar(pago, null, null);

        assertEquals(EstadoPago.VENCIDO, guardado.getEstadoPago());
        verify(pagoRepository).save(pago);
    }

    @Test
    void iniciarPagoConReferenciaExistenteNoLaReemplaza() {
        Usuario usuario = new Usuario();
        usuario.setEmail("residente@example.com");
        residente.setUsuario(usuario);
        Pago pago = new Pago();
        pago.setResidente(residente);
        pago.setMetodo(MetodoPago.PSE);
        pago.setEstadoPago(EstadoPago.VENCIDO);
        pago.setReferenciaPago("PSE-EXISTENTE");
        when(pagoRepository.findById(13L)).thenReturn(Optional.of(pago));

        Pago preparado = pagoService.iniciarPagoOnline(13L, "residente@example.com");

        assertEquals("PSE-EXISTENTE", preparado.getReferenciaPago());
        verify(pagoRepository, never()).save(pago);
    }

    @Test
    void registraResultadoNoAprobadoYConfirmaPagoPagadoSinFecha() {
        Pago pendiente = new Pago();
        pendiente.setEstadoPago(EstadoPago.PENDIENTE);
        when(pagoRepository.save(any(Pago.class))).thenAnswer(invocation -> invocation.getArgument(0));

        pagoService.registrarResultadoSimulado(pendiente, "PENDING", "SIM-PENDING");

        assertEquals(EstadoPago.PENDIENTE, pendiente.getEstadoPago());
        assertEquals(null, pendiente.getFechaPago());

        Pago pagado = new Pago();
        pagado.setEstadoPago(EstadoPago.PAGADO);
        when(pagoRepository.findById(14L)).thenReturn(Optional.of(pagado));
        pagoService.marcarComoPagado(14L);
        assertNotNull(pagado.getFechaPago());
        verify(pagoRepository).save(pagado);
    }

    @Test
    void guardarValidaDatosObligatoriosYAsignaEstadoPorDefecto() {
        Pago pago = new Pago();
        assertThrows(IllegalArgumentException.class, () -> pagoService.guardar(pago, null, null));
        when(residenteRepository.findById(1L)).thenReturn(Optional.of(residente));
        assertThrows(IllegalArgumentException.class, () -> pagoService.guardar(new Pago(), 1L, null));
        when(apartamentoRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> pagoService.guardar(new Pago(), 1L, 10L));

        when(apartamentoRepository.findById(10L)).thenReturn(Optional.of(apartamento));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Pago valido = new Pago();
        Pago guardado = pagoService.guardar(valido, 1L, 10L);
        assertEquals(EstadoPago.PENDIENTE, guardado.getEstadoPago());
    }

    @Test
    void rechazaFechaDeVencimientoAnteriorYBuscaPagoInexistente() {
        when(residenteRepository.findById(1L)).thenReturn(Optional.of(residente));
        when(apartamentoRepository.findById(10L)).thenReturn(Optional.of(apartamento));
        Pago pago = new Pago();
        pago.setFecha(LocalDate.of(2026, 8, 10));
        pago.setFechaVencimiento(LocalDate.of(2026, 8, 9));
        assertThrows(IllegalArgumentException.class, () -> pagoService.guardar(pago, 1L, 10L));
        when(pagoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> pagoService.buscarPorId(99L));
    }

    @Test
    void generaReferenciaDeTarjetaYRechazaEstadosNoCobrables() {
        Usuario usuario = new Usuario();
        usuario.setEmail("residente@example.com");
        residente.setUsuario(usuario);
        Pago tarjeta = new Pago();
        tarjeta.setResidente(residente);
        tarjeta.setMetodo(MetodoPago.TARJETA);
        tarjeta.setEstadoPago(EstadoPago.PENDIENTE);
        when(pagoRepository.findById(12L)).thenReturn(Optional.of(tarjeta));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertTrue(pagoService.iniciarPagoOnline(12L, "RESIDENTE@EXAMPLE.COM").getReferenciaPago().startsWith("CARD-"));

        tarjeta.setEstadoPago(EstadoPago.PAGADO);
        assertThrows(IllegalArgumentException.class, () -> pagoService.iniciarPagoOnline(12L, "residente@example.com"));
        tarjeta.setEstadoPago(EstadoPago.PENDIENTE);
        tarjeta.setMetodo(MetodoPago.EFECTIVO);
        tarjeta.setReferenciaPago(null);
        assertThrows(IllegalArgumentException.class,
                () -> pagoService.iniciarPagoOnline(12L, "residente@example.com"));
    }

    @Test
    void actualizaVencidosYCompletaPagosHistoricosSinFecha() {
        Pago vencido = new Pago();
        vencido.setEstadoPago(EstadoPago.PENDIENTE);
        Pago historico = new Pago();
        historico.setEstadoPago(EstadoPago.PAGADO);
        when(pagoRepository.findByEstadoPagoAndFechaVencimientoBefore(any(), any()))
                .thenReturn(List.of(vencido));
        when(pagoRepository.findByEstadoPagoAndFechaPagoIsNull(EstadoPago.PAGADO))
                .thenReturn(List.of(historico));
        when(pagoRepository.findAll(any(Sort.class))).thenReturn(List.of(vencido, historico));

        pagoService.listarPagos();

        assertEquals(EstadoPago.VENCIDO, vencido.getEstadoPago());
        assertNotNull(historico.getFechaPago());
        verify(pagoRepository).saveAll(List.of(vencido));
        verify(pagoRepository).saveAll(List.of(historico));
    }

    @Test
    void generarAdministracionManejaSinResidentesSinApartamentoYDuplicados() {
        when(residenteRepository.findAll()).thenReturn(List.of());
        assertThrows(IllegalArgumentException.class, () -> pagoService.generarPagosAdministracion());

        Residente sinApartamento = new Residente();
        sinApartamento.setNombre("Sin apartamento");
        Residente existente = new Residente();
        existente.setId(2L);
        existente.setApartamento(apartamento);
        when(residenteRepository.findAll()).thenReturn(List.of(sinApartamento, existente));
        when(pagoRepository.existsByResidenteIdAndTipoPagoAndFechaBetween(any(), any(), any(), any()))
                .thenReturn(true);
        pagoService.generarPagosAdministracion();
        verify(pagoRepository, never()).save(any(Pago.class));
    }
}
