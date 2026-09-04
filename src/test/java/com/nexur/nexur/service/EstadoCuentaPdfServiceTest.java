package com.nexur.nexur.service;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.Pago;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.model.enums.EstadoPago;
import com.nexur.nexur.model.enums.TipoPago;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EstadoCuentaPdfServiceTest {

    @Test
    void generaPdfConElSaldoDelResidente() {
        Apartamento apartamento = new Apartamento();
        apartamento.setNumero("901");
        Residente residente = new Residente("Ana", "12345678", "3001234567", apartamento);
        Pago pago = new Pago();
        pago.setFecha(LocalDate.now());
        pago.setFechaVencimiento(LocalDate.now().plusDays(30));
        pago.setMonto(new BigDecimal("300000"));
        pago.setTipoPago(TipoPago.ADMINISTRACION);
        pago.setEstadoPago(EstadoPago.PENDIENTE);

        byte[] pdf = new EstadoCuentaPdfService().generar(residente, List.of(pago));

        assertTrue(pdf.length > 100);
        assertTrue(new String(pdf, 0, 4).equals("%PDF"));
    }
}
