package com.nexur.nexur.service;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.Pago;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.model.enums.EstadoPago;
import com.nexur.nexur.model.enums.MetodoPago;
import com.nexur.nexur.model.enums.TipoPago;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FacturaPagoPdfServiceTest {

    @Test
    void generaFacturaIndividualConDatosDelPago() throws Exception {
        Apartamento apartamento = new Apartamento();
        apartamento.setNumero("301");
        apartamento.setTorre("B");
        Residente residente = new Residente("Ana Torres", "12345678", "3001234567", apartamento);

        Pago pago = new Pago();
        asignarId(pago, 42L);
        pago.setResidente(residente);
        pago.setApartamento(apartamento);
        pago.setMonto(new BigDecimal("325000"));
        pago.setFecha(LocalDate.of(2026, 8, 1));
        pago.setFechaVencimiento(LocalDate.of(2026, 8, 30));
        pago.setFechaPago(LocalDate.of(2026, 8, 5));
        pago.setTipoPago(TipoPago.ADMINISTRACION);
        pago.setMetodo(MetodoPago.TRANSFERENCIA);
        pago.setEstadoPago(EstadoPago.PAGADO);
        pago.setReferenciaPago("TRF-42");

        byte[] pdf = new FacturaPagoPdfService().generar(pago);

        assertTrue(pdf.length > 100);
        assertTrue(new String(pdf, 0, 4).equals("%PDF"));
    }

    private void asignarId(Pago pago, Long id) throws Exception {
        Field campoId = Pago.class.getDeclaredField("id");
        campoId.setAccessible(true);
        campoId.set(pago, id);
    }
}
