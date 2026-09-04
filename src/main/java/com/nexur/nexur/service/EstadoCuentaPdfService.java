package com.nexur.nexur.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.nexur.nexur.model.Pago;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.model.enums.EstadoPago;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class EstadoCuentaPdfService {

    public byte[] generar(Residente residente, List<Pago> pagos) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        PdfDocument pdf = new PdfDocument(new PdfWriter(salida));
        Document documento = new Document(pdf);

        UrbelixPdfStyle.encabezado(documento, "Estado de cuenta residencial", "Resumen financiero de tu apartamento");
        documento.add(new Paragraph("Residente: " + nombreResidente(residente)));
        documento.add(new Paragraph("Apartamento: " + apartamento(residente)));
        documento.add(new Paragraph("Fecha de generacion: " + LocalDate.now()));

        BigDecimal saldoPendiente = pagos.stream()
                .filter(this::estaPendiente)
                .map(Pago::getMonto)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        documento.add(new Paragraph("Saldo pendiente: $" + saldoPendiente));

        Table tabla = new Table(UnitValue.createPercentArray(new float[]{13f, 19f, 17f, 17f, 17f, 17f}))
                .useAllAvailableWidth();
        tabla.addHeaderCell(UrbelixPdfStyle.encabezadoCelda("Emisión"));
        tabla.addHeaderCell(UrbelixPdfStyle.encabezadoCelda("Tipo"));
        tabla.addHeaderCell(UrbelixPdfStyle.encabezadoCelda("Monto"));
        tabla.addHeaderCell(UrbelixPdfStyle.encabezadoCelda("Vencimiento"));
        tabla.addHeaderCell(UrbelixPdfStyle.encabezadoCelda("Fecha de pago"));
        tabla.addHeaderCell(UrbelixPdfStyle.encabezadoCelda("Estado"));
        for (Pago pago : pagos) {
            tabla.addCell(UrbelixPdfStyle.celda(texto(pago.getFecha())));
            tabla.addCell(UrbelixPdfStyle.celda(pago.getTipoPago() == null ? "Sin tipo" : pago.getTipoPago().name()));
            tabla.addCell(UrbelixPdfStyle.celda("$" + (pago.getMonto() == null ? BigDecimal.ZERO : pago.getMonto())));
            tabla.addCell(UrbelixPdfStyle.celda(texto(pago.getFechaVencimiento())));
            tabla.addCell(UrbelixPdfStyle.celda(texto(pago.getFechaPago())));
            tabla.addCell(UrbelixPdfStyle.celda(pago.getEstadoPago() == null ? "SIN ESTADO" : pago.getEstadoPago().name()));
        }
        documento.add(tabla);
        documento.close();
        return salida.toByteArray();
    }

    private boolean estaPendiente(Pago pago) {
        return pago.getEstadoPago() == EstadoPago.PENDIENTE
                || pago.getEstadoPago() == EstadoPago.VENCIDO;
    }

    private String nombreResidente(Residente residente) {
        return residente == null || residente.getNombre() == null ? "No disponible" : residente.getNombre();
    }

    private String apartamento(Residente residente) {
        if (residente == null || residente.getApartamento() == null) {
            return "No asignado";
        }
        return residente.getApartamento().getNumero();
    }

    private String texto(Object valor) {
        return valor == null ? "-" : valor.toString();
    }
}
