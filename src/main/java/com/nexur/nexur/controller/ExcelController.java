package com.nexur.nexur.controller;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.ImportacionFila;
import com.nexur.nexur.model.ImportacionResultado;
import com.nexur.nexur.repository.ApartamentoRepository;
import com.nexur.nexur.repository.ResidenteRepository;
import com.nexur.nexur.service.AuditoriaService;
import com.nexur.nexur.service.CorreoNotificacionService;
import com.nexur.nexur.service.UsuarioService;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class ExcelController {

    private static final int MAX_FILAS = 1000;
    private static final String[] COLUMNAS = {
            "Nombre", "Documento", "Telefono", "Correo", "Apartamento", "Torre", "Piso", "CodigoRegistro"
    };

    private final ApartamentoRepository apartamentoRepository;
    private final ResidenteRepository residenteRepository;
    private final UsuarioService usuarioService;
    private final CorreoNotificacionService correoNotificacionService;
    private final AuditoriaService auditoriaService;

    public ExcelController(ApartamentoRepository apartamentoRepository,
                           ResidenteRepository residenteRepository,
                           UsuarioService usuarioService,
                           CorreoNotificacionService correoNotificacionService,
                           AuditoriaService auditoriaService) {
        this.apartamentoRepository = apartamentoRepository;
        this.residenteRepository = residenteRepository;
        this.usuarioService = usuarioService;
        this.correoNotificacionService = correoNotificacionService;
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
            Row header = residentes.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            for (int index = 0; index < COLUMNAS.length; index++) {
                header.createCell(index).setCellValue(COLUMNAS[index]);
                header.getCell(index).setCellStyle(headerStyle);
                residentes.autoSizeColumn(index);
            }

            Sheet instrucciones = workbook.createSheet("Instrucciones");
            instrucciones.createRow(0).createCell(0)
                    .setCellValue("Complete la hoja Residentes. No incluya contrasenas.");
            instrucciones.createRow(1).createCell(0)
                    .setCellValue("El apartamento, torre, piso y CodigoRegistro deben coincidir con la base.");
            return excelResponse(workbook, "plantilla_residentes.xlsx");
        }
    }

    @PostMapping("/residentes/importar")
    public String procesarResidentes(@RequestParam("archivo") MultipartFile archivo,
                                     Authentication authentication,
                                     Model model) {
        ImportacionResultado resultado = new ImportacionResultado();
        Set<String> documentos = new HashSet<>();
        Set<String> correos = new HashSet<>();

        if (archivo.isEmpty() || archivo.getOriginalFilename() == null
                || !archivo.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            model.addAttribute("error", "Selecciona un archivo Excel .xlsx valido.");
            model.addAttribute("resultado", resultado);
            return importarResidentes(model);
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook(archivo.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getLastRowNum() > MAX_FILAS) {
                throw new IllegalArgumentException("El archivo no puede superar " + MAX_FILAS + " filas de datos");
            }
            DataFormatter formatter = new DataFormatter();
            validarCabecera(sheet.getRow(0), formatter);

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                String nombre = cell(formatter, row, 0);
                String documento = cell(formatter, row, 1);
                String telefono = cell(formatter, row, 2);
                String correo = cell(formatter, row, 3).toLowerCase(Locale.ROOT);
                String apartamentoNumero = cell(formatter, row, 4);
                String torre = cell(formatter, row, 5);
                String piso = cell(formatter, row, 6);
                String codigoRegistro = cell(formatter, row, 7);

                if (nombre.isBlank() && documento.isBlank() && telefono.isBlank() && correo.isBlank()) {
                    continue;
                }

                resultado.incrementarTotal();
                int fila = rowIndex + 1;
                try {
                    validarFila(nombre, documento, telefono, correo, apartamentoNumero, torre, piso, codigoRegistro);
                    if (!documentos.add(documento)) {
                        resultado.duplicado(new ImportacionFila(fila, documento, nombre, "DUPLICADO",
                                "Documento repetido en el archivo"));
                        continue;
                    }
                    if (!correos.add(correo)) {
                        resultado.duplicado(new ImportacionFila(fila, documento, nombre, "DUPLICADO",
                                "Correo repetido en el archivo"));
                        continue;
                    }
                    if (residenteRepository.existsByDocumento(documento)) {
                        resultado.omitido(new ImportacionFila(fila, documento, nombre, "OMITIDO",
                                "El residente ya existe"));
                        continue;
                    }
                    if (usuarioService.existePorEmail(correo)) {
                        resultado.omitido(new ImportacionFila(fila, documento, nombre, "OMITIDO",
                                "El correo ya esta registrado"));
                        continue;
                    }

                    Apartamento apartamento = apartamentoRepository.findByNumero(apartamentoNumero)
                            .filter(a -> torre.equalsIgnoreCase(a.getTorre())
                                    && piso.equals(String.valueOf(a.getPiso()))
                                    && codigoRegistro.equalsIgnoreCase(a.getCodigoRegistro()))
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Apartamento, torre, piso o codigo residencial no coinciden"));

                    UsuarioService.CuentaImportada cuenta = usuarioService.crearCuentaResidenteImportada(
                            nombre, correo, documento, telefono, apartamento.getNumero(), codigoRegistro);
                    correoNotificacionService.enviarCredencialesIniciales(
                            cuenta.usuario(), cuenta.passwordTemporal());
                    resultado.creado(new ImportacionFila(fila, documento, nombre, "CREADO",
                            "Residente y cuenta creados; credenciales enviadas si SMTP esta activo"));
                } catch (RuntimeException exception) {
                    resultado.error(new ImportacionFila(fila, documento, nombre, "ERROR",
                            limitar(exception.getMessage())));
                }
            }
        } catch (IOException | RuntimeException exception) {
            model.addAttribute("error", exception instanceof IllegalArgumentException
                    ? exception.getMessage()
                    : "No se pudo procesar el archivo Excel.");
        }

        model.addAttribute("resultado", resultado);
        auditoriaService.registrar(authentication == null ? null : authentication.getName(),
                "IMPORTAR_RESIDENTES", "RESIDENTES", null,
                "Lote procesado: " + resultado.getCreados() + " creados, "
                        + resultado.getErrores() + " errores y " + resultado.getDuplicados() + " duplicados");
        return importarResidentes(model);
    }

    private void validarCabecera(Row header, DataFormatter formatter) {
        for (int index = 0; index < COLUMNAS.length; index++) {
            if (header == null || header.getCell(index) == null
                    || !COLUMNAS[index].equalsIgnoreCase(formatter.formatCellValue(header.getCell(index)).trim())) {
                throw new IllegalArgumentException(
                        "Las columnas deben ser: Nombre, Documento, Telefono, Correo, Apartamento, Torre, Piso y CodigoRegistro");
            }
        }
    }

    private void validarFila(String nombre, String documento, String telefono, String correo,
                             String apartamento, String torre, String piso, String codigoRegistro) {
        if (nombre.isBlank() || documento.isBlank() || telefono.isBlank() || correo.isBlank()
                || apartamento.isBlank() || torre.isBlank() || piso.isBlank() || codigoRegistro.isBlank()) {
            throw new IllegalArgumentException("Faltan campos obligatorios");
        }
        if (!documento.matches("\\d{8,}")) {
            throw new IllegalArgumentException("Documento invalido");
        }
        if (!telefono.matches("\\d{10,}")) {
            throw new IllegalArgumentException("Telefono invalido");
        }
        if (!correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Correo invalido");
        }
        try {
            Integer.parseInt(piso);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("El piso debe ser numerico");
        }
    }

    private static String cell(DataFormatter formatter, Row row, int index) {
        return row == null || row.getCell(index) == null
                ? ""
                : formatter.formatCellValue(row.getCell(index)).trim();
    }

    private static String limitar(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            return "Fila invalida";
        }
        return mensaje.length() <= 250 ? mensaje : mensaje.substring(0, 250);
    }

    private static ResponseEntity<byte[]> excelResponse(XSSFWorkbook workbook, String filename) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        workbook.write(output);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).body(output.toByteArray());
    }
}
