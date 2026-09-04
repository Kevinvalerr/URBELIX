package com.nexur.nexur.service;

import com.nexur.nexur.model.Pago;
import com.nexur.nexur.model.ReporteRegistro;
import com.nexur.nexur.model.Residente;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExcelExportService {

    private static final XSSFColor AZUL_URBELIX = new XSSFColor(new byte[]{22, 59, 101}, null);
    private static final XSSFColor VERDE_URBELIX = new XSSFColor(new byte[]{42, (byte) 157, (byte) 143}, null);
    private static final XSSFColor AZUL_CLARO = new XSSFColor(new byte[]{(byte) 244, (byte) 247, (byte) 251}, null);

    public byte[] exportarPagos(List<Pago> pagos) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Pagos");
            Estilos estilos = new Estilos(workbook);
            crearTitulo(sheet, "URBELIX | Reporte de pagos", "Cartera residencial y trazabilidad de pagos", 14, estilos);
            Row encabezado = sheet.createRow(3);
            String[] columnas = {"ID", "Residente", "Apartamento", "Tipo", "Monto", "Metodo", "Fecha emision", "Vencimiento", "Fecha pago", "Referencia", "Resultado sandbox", "Transaccion simulada", "Simulado en", "Estado"};
            for (int columna = 0; columna < columnas.length; columna++) {
                encabezado.createCell(columna).setCellValue(columnas[columna]);
                encabezado.getCell(columna).setCellStyle(estilos.encabezado);
            }
            int fila = 4;
            for (Pago pago : pagos) {
                Row row = sheet.createRow(fila++);
                row.createCell(0).setCellValue(pago.getId() == null ? "" : pago.getId().toString());
                row.createCell(1).setCellValue(pago.getResidente() == null ? "" : pago.getResidente().getNombre());
                row.createCell(2).setCellValue(pago.getApartamento() == null ? "" : pago.getApartamento().getNumero());
                row.createCell(3).setCellValue(pago.getTipoPago() == null ? "" : pago.getTipoPago().name());
                row.createCell(4).setCellValue(pago.getMonto() == null ? 0 : pago.getMonto().doubleValue());
                row.createCell(5).setCellValue(pago.getMetodo() == null ? "" : pago.getMetodo().name());
                row.createCell(6).setCellValue(pago.getFecha() == null ? "" : pago.getFecha().toString());
                row.createCell(7).setCellValue(pago.getFechaVencimiento() == null ? "" : pago.getFechaVencimiento().toString());
                row.createCell(8).setCellValue(pago.getFechaPago() == null ? "" : pago.getFechaPago().toString());
                row.createCell(9).setCellValue(pago.getReferenciaPago() == null ? "" : pago.getReferenciaPago());
                row.createCell(10).setCellValue(pago.getResultadoSimulacion() == null ? "" : pago.getResultadoSimulacion());
                row.createCell(11).setCellValue(pago.getTransaccionSimulada() == null ? "" : pago.getTransaccionSimulada());
                row.createCell(12).setCellValue(pago.getSimuladoEn() == null ? "" : pago.getSimuladoEn().toString());
                row.createCell(13).setCellValue(pago.getEstadoPago() == null ? "" : pago.getEstadoPago().name());
                aplicarEstiloAlterno(row, estilos, fila);
                row.getCell(4).setCellStyle(fila % 2 == 0 ? estilos.monedaAlterna : estilos.moneda);
            }
            prepararHoja(sheet, columnas.length, 3, fila - 1);
            return bytes(workbook);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo generar el Excel de pagos", exception);
        }
    }

    public byte[] exportarResidentes(List<Residente> residentes) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Residentes");
            Estilos estilos = new Estilos(workbook);
            crearTitulo(sheet, "URBELIX | Directorio residencial", "Residentes registrados y apartamento asociado", 6, estilos);
            String[] columnas = {"ID", "Nombre", "Documento", "Telefono", "Apartamento", "Correo"};
            Row encabezado = sheet.createRow(3);
            for (int columna = 0; columna < columnas.length; columna++) {
                encabezado.createCell(columna).setCellValue(columnas[columna]);
                encabezado.getCell(columna).setCellStyle(estilos.encabezado);
            }
            int fila = 4;
            for (Residente residente : residentes) {
                Row row = sheet.createRow(fila++);
                row.createCell(0).setCellValue(residente.getId() == null ? "" : residente.getId().toString());
                row.createCell(1).setCellValue(residente.getNombre() == null ? "" : residente.getNombre());
                row.createCell(2).setCellValue(residente.getDocumento() == null ? "" : residente.getDocumento());
                row.createCell(3).setCellValue(residente.getTelefono() == null ? "" : residente.getTelefono());
                row.createCell(4).setCellValue(residente.getApartamento() == null ? "" : residente.getApartamento().getNumero());
                row.createCell(5).setCellValue(residente.getUsuario() == null ? "" : residente.getUsuario().getEmail());
                aplicarEstiloAlterno(row, estilos, fila);
            }
            prepararHoja(sheet, columnas.length, 3, fila - 1);
            return bytes(workbook);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo generar el Excel de residentes", exception);
        }
    }

    public byte[] exportarReporte(List<ReporteRegistro> registros, String tipo,
                                  String fechaInicio, String fechaFin) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Reporte");
            Estilos estilos = new Estilos(workbook);
            String periodo = (fechaInicio == null || fechaInicio.isBlank() ? "Inicio" : fechaInicio)
                    + " a " + (fechaFin == null || fechaFin.isBlank() ? "Hoy" : fechaFin);
            crearTitulo(sheet, "URBELIX | Reporte operativo", "Tipo: "
                    + (tipo == null || tipo.isBlank() ? "TODOS" : tipo) + " | Periodo: " + periodo,
                    5, estilos);
            String[] columnas = {"Tipo", "Referencia", "Residente / visitante", "Descripcion", "Fecha"};
            Row encabezado = sheet.createRow(3);
            for (int columna = 0; columna < columnas.length; columna++) {
                encabezado.createCell(columna).setCellValue(columnas[columna]);
                encabezado.getCell(columna).setCellStyle(estilos.encabezado);
            }
            int fila = 4;
            for (ReporteRegistro registro : registros) {
                Row row = sheet.createRow(fila++);
                row.createCell(0).setCellValue(valor(registro.getTipo()));
                row.createCell(1).setCellValue(valor(registro.getEntidad()));
                row.createCell(2).setCellValue(valor(registro.getResidente()));
                row.createCell(3).setCellValue(valor(registro.getDescripcion()));
                row.createCell(4).setCellValue(registro.getFechaHora() == null ? "" : registro.getFechaHora().toString());
                aplicarEstiloAlterno(row, estilos, fila);
            }
            prepararHoja(sheet, columnas.length, 3, fila - 1);
            return bytes(workbook);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo generar el Excel del reporte", exception);
        }
    }

    private byte[] bytes(Workbook workbook) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        workbook.write(output);
        return output.toByteArray();
    }

    private void crearTitulo(Sheet sheet, String titulo, String subtitulo, int columnas, Estilos estilos) {
        Row tituloRow = sheet.createRow(0);
        for (int columna = 0; columna < columnas; columna++) {
            tituloRow.createCell(columna).setCellStyle(estilos.titulo);
        }
        tituloRow.getCell(0).setCellValue(titulo);
        Row subtituloRow = sheet.createRow(1);
        for (int columna = 0; columna < columnas; columna++) {
            subtituloRow.createCell(columna).setCellStyle(estilos.subtitulo);
        }
        subtituloRow.getCell(0).setCellValue(subtitulo);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, columnas - 1));
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, columnas - 1));
    }

    private void prepararHoja(Sheet sheet, int cantidad, int filaEncabezado, int ultimaFila) {
        for (int columna = 0; columna < cantidad; columna++) {
            sheet.autoSizeColumn(columna);
            sheet.setColumnWidth(columna, Math.min(sheet.getColumnWidth(columna) + 900, 18000));
        }
        sheet.createFreezePane(0, filaEncabezado + 1);
        if (ultimaFila >= filaEncabezado + 1) {
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(filaEncabezado, ultimaFila, 0, cantidad - 1));
        }
    }

    private void aplicarEstiloAlterno(Row row, Estilos estilos, int fila) {
        if (fila % 2 == 0) {
            for (int columna = 0; columna < row.getLastCellNum(); columna++) {
                row.getCell(columna).setCellStyle(estilos.alterno);
            }
        }
    }

    private String valor(String valor) {
        return valor == null ? "" : valor;
    }

    private static class Estilos {
        private final CellStyle titulo;
        private final CellStyle subtitulo;
        private final CellStyle encabezado;
        private final CellStyle alterno;
        private final CellStyle moneda;
        private final CellStyle monedaAlterna;

        private Estilos(Workbook workbook) {
            Font fuenteTitulo = workbook.createFont();
            fuenteTitulo.setBold(true);
            fuenteTitulo.setFontHeightInPoints((short) 16);
            fuenteTitulo.setColor(IndexedColors.WHITE.getIndex());
            titulo = workbook.createCellStyle();
            titulo.setFont(fuenteTitulo);
            titulo.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            titulo.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Font fuenteSubtitulo = workbook.createFont();
            fuenteSubtitulo.setItalic(true);
            fuenteSubtitulo.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            subtitulo = workbook.createCellStyle();
            subtitulo.setFont(fuenteSubtitulo);

            Font fuenteEncabezado = workbook.createFont();
            fuenteEncabezado.setBold(true);
            fuenteEncabezado.setColor(IndexedColors.WHITE.getIndex());
            encabezado = workbook.createCellStyle();
            encabezado.setFont(fuenteEncabezado);
            encabezado.setAlignment(HorizontalAlignment.CENTER);
            encabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            alterno = workbook.createCellStyle();
            ((XSSFCellStyle) alterno).setFillForegroundColor(AZUL_CLARO);
            alterno.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            DataFormat formato = workbook.createDataFormat();
            moneda = workbook.createCellStyle();
            moneda.setDataFormat(formato.getFormat("$ #,##0"));
            monedaAlterna = workbook.createCellStyle();
            monedaAlterna.cloneStyleFrom(alterno);
            monedaAlterna.setDataFormat(formato.getFormat("$ #,##0"));

            for (CellStyle estilo : new CellStyle[]{titulo, subtitulo, encabezado, alterno, moneda, monedaAlterna}) {
                estilo.setVerticalAlignment(VerticalAlignment.CENTER);
                estilo.setBorderBottom(BorderStyle.THIN);
                ((XSSFCellStyle) estilo).setBottomBorderColor(AZUL_CLARO);
            }
            ((XSSFCellStyle) encabezado).setFillForegroundColor(VERDE_URBELIX);
            encabezado.setWrapText(true);
            ((XSSFCellStyle) titulo).setFillForegroundColor(AZUL_URBELIX);
            titulo.setAlignment(HorizontalAlignment.LEFT);
        }
    }
}
