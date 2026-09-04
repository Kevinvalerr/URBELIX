package com.nexur.nexur.service;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.Pago;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.model.enums.EstadoPago;
import com.nexur.nexur.model.enums.MetodoPago;
import com.nexur.nexur.model.enums.TipoPago;
import com.nexur.nexur.model.ReporteRegistro;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelExportServiceTest {
    private final ExcelExportService service = new ExcelExportService();

    @Test
    void exportaPagosConDatosCompletosYCamposVacios() throws Exception {
        Pago completo = new Pago();
        completo.setMonto(new BigDecimal("300000"));
        completo.setFecha(LocalDate.of(2026, 8, 1));
        completo.setFechaVencimiento(LocalDate.of(2026, 8, 31));
        completo.setFechaPago(LocalDate.of(2026, 8, 10));
        completo.setEstadoPago(EstadoPago.PAGADO);
        completo.setMetodo(MetodoPago.TRANSFERENCIA);
        completo.setTipoPago(TipoPago.ADMINISTRACION);
        Residente residente = new Residente();
        residente.setNombre("Ana");
        Apartamento apartamento = new Apartamento();
        apartamento.setNumero("101");
        completo.setResidente(residente);
        completo.setApartamento(apartamento);

        Pago vacio = new Pago();
        byte[] archivo = service.exportarPagos(List.of(completo, vacio));

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(archivo))) {
            var sheet = workbook.getSheet("Pagos");
            assertEquals(6, sheet.getLastRowNum() + 1);
            assertEquals("ID", sheet.getRow(3).getCell(0).getStringCellValue());
            assertEquals("Ana", sheet.getRow(4).getCell(1).getStringCellValue());
            assertEquals(300000D, sheet.getRow(4).getCell(4).getNumericCellValue());
            assertEquals("", sheet.getRow(5).getCell(0).getStringCellValue());
        }
    }

    @Test
    void exportaResidentesConYsinRelaciones() throws Exception {
        Residente completo = new Residente();
        completo.setId(2L);
        completo.setNombre("Carlos");
        completo.setDocumento("12345678");
        completo.setTelefono("3001234567");
        Apartamento apartamento = new Apartamento();
        apartamento.setNumero("202");
        completo.setApartamento(apartamento);
        Usuario usuario = new Usuario();
        usuario.setEmail("carlos@example.com");
        completo.setUsuario(usuario);

        byte[] archivo = service.exportarResidentes(List.of(completo, new Residente()));

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(archivo))) {
            var sheet = workbook.getSheet("Residentes");
            assertEquals(6, sheet.getLastRowNum() + 1);
            assertEquals("Carlos", sheet.getRow(4).getCell(1).getStringCellValue());
            assertEquals("carlos@example.com", sheet.getRow(4).getCell(5).getStringCellValue());
            assertTrue(sheet.getRow(5).getCell(1).getStringCellValue().isEmpty());
        }
    }

    @Test
    void exportaReporteConFiltrosVaciosYRegistrosConYsinDatos() throws Exception {
        ReporteRegistro completo = new ReporteRegistro("PAGO", "P-10", "Ana",
                "Pago aprobado", LocalDateTime.of(2026, 8, 10, 9, 30));

        ReporteRegistro vacio = new ReporteRegistro(null, null, null, null, null);
        byte[] archivo = service.exportarReporte(List.of(completo, vacio), " ", null, " ");

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(archivo))) {
            var sheet = workbook.getSheet("Reporte");
            assertEquals("PAGO", sheet.getRow(4).getCell(0).getStringCellValue());
            assertEquals("", sheet.getRow(5).getCell(0).getStringCellValue());
            assertEquals("2026-08-10T09:30", sheet.getRow(4).getCell(4).getStringCellValue());
            assertEquals("", sheet.getRow(5).getCell(4).getStringCellValue());
        }
    }
}
