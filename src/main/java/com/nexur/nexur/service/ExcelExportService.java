package com.nexur.nexur.service;

import com.nexur.nexur.model.Pago;
import com.nexur.nexur.model.Residente;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExcelExportService {

    public byte[] exportarPagos(List<Pago> pagos) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Pagos");
            Row encabezado = sheet.createRow(0);
            String[] columnas = {"ID", "Residente", "Apartamento", "Tipo", "Monto", "Metodo", "Fecha emision", "Vencimiento", "Fecha pago", "Estado"};
            for (int columna = 0; columna < columnas.length; columna++) {
                encabezado.createCell(columna).setCellValue(columnas[columna]);
            }
            int fila = 1;
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
                row.createCell(9).setCellValue(pago.getEstadoPago() == null ? "" : pago.getEstadoPago().name());
            }
            ajustarColumnas(sheet, columnas.length);
            return bytes(workbook);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo generar el Excel de pagos", exception);
        }
    }

    public byte[] exportarResidentes(List<Residente> residentes) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Residentes");
            String[] columnas = {"ID", "Nombre", "Documento", "Telefono", "Apartamento", "Correo"};
            Row encabezado = sheet.createRow(0);
            for (int columna = 0; columna < columnas.length; columna++) {
                encabezado.createCell(columna).setCellValue(columnas[columna]);
            }
            int fila = 1;
            for (Residente residente : residentes) {
                Row row = sheet.createRow(fila++);
                row.createCell(0).setCellValue(residente.getId() == null ? "" : residente.getId().toString());
                row.createCell(1).setCellValue(residente.getNombre() == null ? "" : residente.getNombre());
                row.createCell(2).setCellValue(residente.getDocumento() == null ? "" : residente.getDocumento());
                row.createCell(3).setCellValue(residente.getTelefono() == null ? "" : residente.getTelefono());
                row.createCell(4).setCellValue(residente.getApartamento() == null ? "" : residente.getApartamento().getNumero());
                row.createCell(5).setCellValue(residente.getUsuario() == null ? "" : residente.getUsuario().getEmail());
            }
            ajustarColumnas(sheet, columnas.length);
            return bytes(workbook);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo generar el Excel de residentes", exception);
        }
    }

    private byte[] bytes(Workbook workbook) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        workbook.write(output);
        return output.toByteArray();
    }

    private void ajustarColumnas(Sheet sheet, int cantidad) {
        for (int columna = 0; columna < cantidad; columna++) {
            sheet.autoSizeColumn(columna);
        }
    }
}
