package com.urbelix.urbelix.controller;

import com.urbelix.urbelix.model.Pago;
import com.urbelix.urbelix.model.Residente;
import com.urbelix.urbelix.service.PagoService;
import com.urbelix.urbelix.service.ResidenteService;
import com.urbelix.urbelix.model.Apartamento;
import com.urbelix.urbelix.model.ImportacionFila;
import com.urbelix.urbelix.model.ImportacionResultado;
import com.urbelix.urbelix.repository.ApartamentoRepository;
import com.urbelix.urbelix.service.AuditoriaService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.time.LocalDate;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class ExcelController {

    private final PagoService pagoService;
    private final ResidenteService residenteService;
    private final ApartamentoRepository apartamentoRepository;
    private final AuditoriaService auditoriaService;

    public ExcelController(PagoService pagoService, ResidenteService residenteService,
                           ApartamentoRepository apartamentoRepository,
                           AuditoriaService auditoriaService) {
        this.pagoService = pagoService;
        this.residenteService = residenteService;
        this.apartamentoRepository = apartamentoRepository;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping("/residentes/importar")
    public String importarResidentes(Model model) {
        model.addAttribute("titulo", "Importar residentes");
        model.addAttribute("currentPath", "/residentes/importar");
        return "residentes/importar";
    }

    @GetMapping("/residentes/importar/plantilla")
    public ResponseEntity<byte[]> plantillaResidentes() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet residentes = workbook.createSheet("Residentes");
            String[] columns = {"Nombre", "Documento", "Telefono", "Correo", "Apartamento", "Torre", "Piso"};
            Row header = residentes.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            for (int index = 0; index < columns.length; index++) {
                header.createCell(index).setCellValue(columns[index]);
                header.getCell(index).setCellStyle(headerStyle);
                residentes.autoSizeColumn(index);
            }
            Sheet instrucciones = workbook.createSheet("Instrucciones");
            instrucciones.createRow(0).createCell(0).setCellValue("Complete la hoja Residentes. Todos los campos son obligatorios. No incluya contraseñas.");
            instrucciones.createRow(1).createCell(0).setCellValue("El apartamento debe existir y Torre/Piso deben coincidir con la base de datos.");
            return excelResponse(workbook, "plantilla_residentes.xlsx");
        }
    }

    @PostMapping("/residentes/importar")
    public String procesarResidentes(@RequestParam("archivo") MultipartFile archivo, Model model) {
        ImportacionResultado resultado = new ImportacionResultado();
        Set<String> documentos = new HashSet<>();
        Set<String> correos = new HashSet<>();

        if (archivo.isEmpty() || archivo.getOriginalFilename() == null
                || !archivo.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            model.addAttribute("error", "Selecciona un archivo Excel .xlsx válido.");
            model.addAttribute("resultado", resultado);
            return importarResidentes(model);
        }

        DataFormatter formatter = new DataFormatter();
        try (XSSFWorkbook workbook = new XSSFWorkbook(archivo.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            String[] expected = {"nombre", "documento", "telefono", "correo", "apartamento", "torre", "piso"};
            for (int index = 0; index < expected.length; index++) {
                if (header == null || header.getCell(index) == null
                        || !expected[index].equalsIgnoreCase(formatter.formatCellValue(header.getCell(index)).trim())) {
                    model.addAttribute("error", "El archivo debe contener las columnas: Nombre, Documento, Telefono, Correo, Apartamento, Torre y Piso.");
                    model.addAttribute("resultado", resultado);
                    return importarResidentes(model);
                }
            }

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                String nombre = cell(formatter, row, 0);
                String documento = cell(formatter, row, 1);
                String telefono = cell(formatter, row, 2);
                String correo = cell(formatter, row, 3).toLowerCase(Locale.ROOT);
                String apartamentoNumero = cell(formatter, row, 4);
                String torre = cell(formatter, row, 5);
                String piso = cell(formatter, row, 6);
                if (nombre.isBlank() && documento.isBlank() && telefono.isBlank() && correo.isBlank()) {
                    continue;
                }
                resultado.incrementarTotal();
                int fila = rowIndex + 1;
                try {
                    if (nombre.isBlank() || documento.isBlank() || telefono.isBlank() || correo.isBlank()
                            || apartamentoNumero.isBlank() || torre.isBlank() || piso.isBlank()) {
                        throw new IllegalArgumentException("Faltan campos obligatorios");
                    }
                    if (!documento.matches("\\d{8,}")) throw new IllegalArgumentException("Documento inválido");
                    if (!telefono.matches("\\d{10,}")) throw new IllegalArgumentException("Teléfono inválido");
                    if (!correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) throw new IllegalArgumentException("Correo inválido");
                    if (!documentos.add(documento)) {
                        resultado.duplicado(new ImportacionFila(fila, documento, nombre, "DUPLICADO", "Documento repetido en el archivo"));
                        continue;
                    }
                    if (!correos.add(correo)) {
                        resultado.duplicado(new ImportacionFila(fila, documento, nombre, "DUPLICADO", "Correo repetido en el archivo"));
                        continue;
                    }
                    if (residenteService.obtenerTodos().stream().anyMatch(r -> documento.equals(r.getDocumento()))) {
                        resultado.omitido(new ImportacionFila(fila, documento, nombre, "OMITIDO", "El residente ya existe"));
                        continue;
                    }
                    Apartamento apartamento = apartamentoRepository.findByNumero(apartamentoNumero)
                            .filter(a -> torre.equalsIgnoreCase(a.getTorre())
                                    && piso.equals(String.valueOf(a.getPiso())))
                            .orElseThrow(() -> new IllegalArgumentException("Apartamento no encontrado o torre/piso no coinciden"));
                    Residente residente = new Residente();
                    residente.setNombre(nombre);
                    residente.setDocumento(documento);
                    residente.setTelefono(telefono);
                    residente.setCorreo(correo);
                    residenteService.crearConCuenta(residente, List.of(apartamento.getId()));
                    resultado.creado(new ImportacionFila(fila, documento, nombre, "CREADO", "Residente y usuario creados"));
                } catch (RuntimeException ex) {
                    resultado.error(new ImportacionFila(fila, documento, nombre, "ERROR", ex.getMessage()));
                }
            }
        } catch (IOException | RuntimeException ex) {
            model.addAttribute("error", "No se pudo procesar el archivo Excel.");
        }
        model.addAttribute("resultado", resultado);
        auditoriaService.registrar("IMPORTAR_RESIDENTES", "RESIDENTES", "ImportacionExcel", null,
            "EXITO", "Lote procesado: " + resultado.getCreados() + " creados, "
                + resultado.getErrores() + " errores y " + resultado.getDuplicados() + " duplicados");
        return importarResidentes(model);
    }

    private static String cell(DataFormatter formatter, Row row, int index) {
        return row == null || row.getCell(index) == null ? "" : formatter.formatCellValue(row.getCell(index)).trim();
    }

    @GetMapping("/excel/pagos")
    public ResponseEntity<byte[]> exportarPagos(@RequestParam(required = false) String texto,
                                                @RequestParam(required = false) String estado,
                                                @RequestParam(required = false) String torre,
                                                @RequestParam(required = false) String apartamento,
                                                @RequestParam(required = false) Long residenteId,
                                                @RequestParam(required = false) LocalDate fechaDesde,
                                                @RequestParam(required = false) LocalDate fechaHasta) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Pagos");
            Row title = sheet.createRow(0);
            title.createCell(0).setCellValue("URBELIX - REPORTE DE PAGOS");
            Row header = sheet.createRow(2);
            String[] columns = {"ID", "Residente", "Apartamento", "Tipo", "Monto", "Metodo", "Fecha", "Vencimiento", "Estado"};
            writeHeaders(header, columns);

            int rowNumber = 3;
            BigDecimal total = BigDecimal.ZERO;
            for (Pago pago : pagoService.listarPagos()) {
                if (!coincidePago(pago, texto, estado, torre, apartamento, residenteId, fechaDesde, fechaHasta)) continue;
                Row row = sheet.createRow(rowNumber++);
                row.createCell(0).setCellValue(value(pago.getId()));
                row.createCell(1).setCellValue(pago.getResidente() != null ? value(pago.getResidente().getNombre()) : "");
                row.createCell(2).setCellValue(pago.getApartamento() != null ? value(pago.getApartamento().getNumero()) : "");
                row.createCell(3).setCellValue(pago.getTipoPago() != null ? pago.getTipoPago().name() : "");
                setDecimal(row, 4, pago.getMonto());
                row.createCell(5).setCellValue(pago.getMetodo() != null ? pago.getMetodo().name() : "");
                row.createCell(6).setCellValue(pago.getFecha() != null ? pago.getFecha().toString() : "");
                row.createCell(7).setCellValue(pago.getFechaVencimiento() != null ? pago.getFechaVencimiento().toString() : "");
                row.createCell(8).setCellValue(pago.getEstadoPago() != null ? pago.getEstadoPago().name() : "");
                total = total.add(pago.getMonto() == null ? BigDecimal.ZERO : pago.getMonto());
            }
            Row totalRow = sheet.createRow(rowNumber + 1);
            totalRow.createCell(0).setCellValue("TOTAL REGISTROS");
            totalRow.createCell(1).setCellValue(rowNumber - 3);
            totalRow.createCell(3).setCellValue("TOTAL MONTO");
            totalRow.createCell(4).setCellValue(total.doubleValue());
            sheet.createFreezePane(0, 3);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(2, Math.max(2, rowNumber - 1), 0, columns.length - 1));
            autosize(sheet, columns.length);
            return excelResponse(workbook, "pagos.xlsx");
        }
    }

    private static boolean coincidePago(Pago pago, String texto, String estado, String torre, String apartamento,
                                        Long residenteId, LocalDate fechaDesde, LocalDate fechaHasta) {
        String busqueda = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        String residente = pago.getResidente() == null ? "" : (value(pago.getResidente().getNombre()) + " " + value(pago.getResidente().getDocumento())).toLowerCase(Locale.ROOT);
        String numero = pago.getApartamento() == null ? "" : value(pago.getApartamento().getNumero());
        String torrePago = pago.getApartamento() == null ? "" : value(pago.getApartamento().getTorre());
        return (busqueda.isBlank() || residente.contains(busqueda) || numero.toLowerCase(Locale.ROOT).contains(busqueda))
                && (estado == null || estado.isBlank() || (pago.getEstadoPago() != null && estado.equalsIgnoreCase(pago.getEstadoPago().name())))
                && (torre == null || torre.isBlank() || torre.equalsIgnoreCase(torrePago))
                && (apartamento == null || apartamento.isBlank() || apartamento.equalsIgnoreCase(numero))
                && (residenteId == null || pago.getResidente() != null && residenteId.equals(pago.getResidente().getId()))
                && (fechaDesde == null || pago.getFecha() != null && !pago.getFecha().isBefore(fechaDesde))
                && (fechaHasta == null || pago.getFecha() != null && !pago.getFecha().isAfter(fechaHasta));
    }

    @GetMapping("/excel/residentes")
    public ResponseEntity<byte[]> exportarResidentes() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Residentes");
            Row header = sheet.createRow(0);
            String[] columns = {"ID", "Nombre", "Documento", "Telefono", "Apartamento", "Torre", "Email"};
            writeHeaders(header, columns);

            int rowNumber = 1;
            for (Residente residente : residenteService.obtenerTodos()) {
                Row row = sheet.createRow(rowNumber++);
                row.createCell(0).setCellValue(value(residente.getId()));
                row.createCell(1).setCellValue(value(residente.getNombre()));
                row.createCell(2).setCellValue(value(residente.getDocumento()));
                row.createCell(3).setCellValue(value(residente.getTelefono()));
                row.createCell(4).setCellValue(residente.getApartamento() != null ? value(residente.getApartamento().getNumero()) : "");
                row.createCell(5).setCellValue(residente.getApartamento() != null ? value(residente.getApartamento().getTorre()) : "");
                row.createCell(6).setCellValue(residente.getUsuario() != null ? value(residente.getUsuario().getEmail()) : "");
            }
            autosize(sheet, columns.length);
            return excelResponse(workbook, "residentes.xlsx");
        }
    }

    private static void writeHeaders(Row row, String[] columns) {
        for (int index = 0; index < columns.length; index++) {
            row.createCell(index).setCellValue(columns[index]);
        }
    }

    private static void autosize(Sheet sheet, int columnCount) {
        for (int index = 0; index < columnCount; index++) {
            sheet.autoSizeColumn(index);
        }
    }

    private static void setDecimal(Row row, int column, BigDecimal value) {
        if (value != null) {
            row.createCell(column).setCellValue(value.doubleValue());
        } else {
            row.createCell(column).setCellValue(0);
        }
    }

    private static String value(Object value) {
        return value == null ? "" : value.toString();
    }

    private static ResponseEntity<byte[]> excelResponse(XSSFWorkbook workbook, String filename) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        workbook.write(output);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).body(output.toByteArray());
    }
}
