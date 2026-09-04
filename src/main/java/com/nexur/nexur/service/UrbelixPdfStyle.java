package com.nexur.nexur.service;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

public final class UrbelixPdfStyle {

    public static final Color AZUL = new DeviceRgb(22, 59, 101);
    public static final Color VERDE = new DeviceRgb(42, 157, 143);
    public static final Color FONDO = new DeviceRgb(244, 247, 251);

    private UrbelixPdfStyle() {
    }

    public static void encabezado(Document documento, String titulo, String subtitulo) {
        documento.add(new Paragraph("URBELIX").setBold().setFontSize(20).setFontColor(AZUL));
        documento.add(new Paragraph(titulo).setBold().setFontSize(15).setFontColor(AZUL));
        documento.add(new Paragraph(subtitulo).setFontSize(9).setFontColor(new DeviceRgb(82, 98, 115))
                .setMarginBottom(14));
    }

    public static Table tabla(float[] columnas) {
        return new Table(UnitValue.createPercentArray(columnas)).useAllAvailableWidth();
    }

    public static Cell encabezadoCelda(String texto) {
        return new Cell().add(new Paragraph(texto).setBold().setFontSize(8))
                .setBackgroundColor(AZUL).setFontColor(com.itextpdf.kernel.colors.ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER);
    }

    public static Cell celda(String texto) {
        return new Cell().add(new Paragraph(texto == null ? "-" : texto).setFontSize(8));
    }
}