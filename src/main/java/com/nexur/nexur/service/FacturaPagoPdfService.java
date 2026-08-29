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
import com.nexur.nexur.model.enums.MetodoPago;
import com.nexur.nexur.model.enums.TipoPago;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class FacturaPagoPdfService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale LOCALE_COLOMBIA = Locale.of("es", "CO");

    public byte[] generar(Pago pago) {
        if (pago == null || pago.getId() == null) {
            throw new IllegalArgumentException("No se puede generar una factura sin un pago persistido");
        }

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        PdfDocument pdf = new PdfDocument(new PdfWriter(salida));
        Document documento = new Document(pdf);

        documento.add(new Paragraph("URBELIX")
                .setBold()
                .setFontSize(20));
        documento.add(new Paragraph("Factura / comprobante de pago")
                .setBold()
                .setFontSize(16));
        documento.add(new Paragraph("Documento interno de cobranza residencial")
                .setFontSize(10));

        Table identificacion = tabla(new float[]{32f, 68f});
        fila(identificacion, "Número", numeroFactura(pago));
        fila(identificacion, "Generada", formato(LocalDate.now()));
        fila(identificacion, "Referencia", texto(pago.getReferenciaPago()));
        documento.add(identificacion);

        documento.add(new Paragraph("Datos del residente").setBold().setFontSize(13));
        Table residenteTabla = tabla(new float[]{32f, 68f});
        Residente residente = pago.getResidente();
        fila(residenteTabla, "Residente", residente == null ? "No disponible" : texto(residente.getNombre()));
        fila(residenteTabla, "Documento", residente == null ? "No disponible" : texto(residente.getDocumento()));
        fila(residenteTabla, "Apartamento", apartamento(pago));
        documento.add(residenteTabla);

        documento.add(new Paragraph("Detalle de la obligación").setBold().setFontSize(13));
        Table detalle = tabla(new float[]{32f, 68f});
        fila(detalle, "Concepto", tipo(pago.getTipoPago()));
        fila(detalle, "Método", metodo(pago.getMetodo()));
        fila(detalle, "Fecha de emisión", formato(pago.getFecha()));
        fila(detalle, "Fecha de vencimiento", formato(pago.getFechaVencimiento()));
        fila(detalle, "Fecha de pago", formato(pago.getFechaPago()));
        fila(detalle, "Estado", estado(pago.getEstadoPago()));
        fila(detalle, "Total", moneda(pago.getMonto()) + " COP");
        documento.add(detalle);

        documento.add(new Paragraph("Gracias por utilizar Urbelix.").setMarginTop(18));
        documento.add(new Paragraph("Este documento no constituye una factura electrónica DIAN.")
                .setFontSize(8));
        documento.close();
        return salida.toByteArray();
    }

    private Table tabla(float[] columnas) {
        return new Table(UnitValue.createPercentArray(columnas)).useAllAvailableWidth();
    }

    private void fila(Table tabla, String etiqueta, String valor) {
        tabla.addCell(new Paragraph(etiqueta).setBold());
        tabla.addCell(valor);
    }

    private String numeroFactura(Pago pago) {
        return String.format(Locale.ROOT, "URB-%08d", pago.getId());
    }

    private String apartamento(Pago pago) {
        if (pago.getApartamento() == null) {
            return "No asignado";
        }
        String numero = texto(pago.getApartamento().getNumero());
        String torre = pago.getApartamento().getTorre();
        return numero + (torre == null || torre.isBlank() ? "" : " - Torre " + torre);
    }

    private String tipo(TipoPago tipo) {
        return tipo == null ? "Sin tipo" : tipo.getDescripcion();
    }

    private String metodo(MetodoPago metodo) {
        return metodo == null ? "Sin método" : metodo.getDescripcion();
    }

    private String estado(EstadoPago estado) {
        if (estado == null) {
            return "Sin estado";
        }
        return switch (estado) {
            case PAGADO -> "Pagado";
            case PENDIENTE -> "Pendiente";
            case VENCIDO -> "Vencido";
        };
    }

    private String moneda(BigDecimal monto) {
        NumberFormat formato = NumberFormat.getNumberInstance(LOCALE_COLOMBIA);
        formato.setMinimumFractionDigits(0);
        formato.setMaximumFractionDigits(0);
        return "$ " + formato.format(monto == null ? BigDecimal.ZERO : monto);
    }

    private String formato(LocalDate fecha) {
        return fecha == null ? "No registrada" : fecha.format(FORMATO_FECHA);
    }

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? "No disponible" : valor;
    }
}
