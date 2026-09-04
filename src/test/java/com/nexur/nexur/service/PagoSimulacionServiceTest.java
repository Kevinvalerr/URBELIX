package com.nexur.nexur.service;

import com.nexur.nexur.model.Pago;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.model.enums.EstadoPago;
import com.nexur.nexur.model.enums.MetodoPago;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoSimulacionServiceTest {
    @Mock private PagoService pagoService;

    @Test
    void aprobacionLocalConfirmaPago() {
        Pago pago = pagoPendiente();
        when(pagoService.buscarPorId(8L)).thenReturn(pago);
        PagoSimulacionService service = new PagoSimulacionService(pagoService, true);

        PagoSimulacionService.Resultado resultado = service.simular(8L, "residente@example.com", "APPROVED");

        assertEquals("APPROVED", resultado.estadoProveedor());
        verify(pagoService).registrarResultadoSimulado(pago, "APPROVED", resultado.transactionId());
    }

    @Test
    void escenariosNoAprobadosNoConfirmanPago() {
        Pago pago = pagoPendiente();
        when(pagoService.buscarPorId(8L)).thenReturn(pago);
        PagoSimulacionService service = new PagoSimulacionService(pagoService, true);

        assertEquals("PENDING", service.simular(8L, "residente@example.com", "PENDIENTE").estadoProveedor());
        assertEquals("DECLINED", service.simular(8L, "residente@example.com", "RECHAZADO").estadoProveedor());
        assertEquals("VOIDED", service.simular(8L, "residente@example.com", "ANULADO").estadoProveedor());
        assertEquals("ERROR", service.simular(8L, "residente@example.com", "ERROR").estadoProveedor());
        verify(pagoService, org.mockito.Mockito.times(4))
                .registrarResultadoSimulado(org.mockito.Mockito.eq(pago), org.mockito.Mockito.anyString(),
                        org.mockito.Mockito.anyString());
    }

    @Test
    void rechazaPropietarioDiferenteResultadoInvalidoYPagoNoOnline() {
        Pago pago = pagoPendiente();
        when(pagoService.buscarPorId(8L)).thenReturn(pago);
        PagoSimulacionService service = new PagoSimulacionService(pagoService, true);

        assertThrows(IllegalArgumentException.class, () -> service.simular(8L, "otro@example.com", "APPROVED"));
        assertThrows(IllegalArgumentException.class, () -> service.simular(8L, "residente@example.com", "CUALQUIERA"));
        pago.setMetodo(MetodoPago.EFECTIVO);
        assertThrows(IllegalArgumentException.class,
                () -> service.simular(8L, "residente@example.com", "APPROVED"));
    }

    @Test
    void rechazaSandboxDesactivado() {
        PagoSimulacionService service = new PagoSimulacionService(pagoService, false);
        assertThrows(IllegalStateException.class, () -> service.simular(8L, "residente@example.com", "APPROVED"));
    }

    private Pago pagoPendiente() {
        Usuario usuario = new Usuario();
        usuario.setEmail("residente@example.com");
        Residente residente = new Residente();
        residente.setId(4L);
        residente.setUsuario(usuario);
        Pago pago = new Pago();
        pago.setMonto(new BigDecimal("100000"));
        pago.setEstadoPago(EstadoPago.PENDIENTE);
        pago.setMetodo(MetodoPago.PSE);
        pago.setReferenciaPago("PSE-TEST");
        pago.setResidente(residente);
        return pago;
    }
}
