package com.nexur.nexur.controller;

import com.nexur.nexur.model.ReporteRegistro;
import com.nexur.nexur.service.ReporteService;
import com.nexur.nexur.service.ApartamentoService;
import com.nexur.nexur.service.PagoService;
import com.nexur.nexur.service.ReservaService;
import com.nexur.nexur.service.ResidenteService;
import com.nexur.nexur.service.ReportesFastApiService;
import com.nexur.nexur.service.ExcelExportService;
import com.nexur.nexur.model.enums.EstadoPago;
import com.nexur.nexur.model.enums.EstadoReserva;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import java.io.ByteArrayOutputStream;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class ReporteController {

    private static final Logger log = LoggerFactory.getLogger(ReporteController.class);

    private final ReporteService reporteService;
    private final ResidenteService residenteService;
    private final ApartamentoService apartamentoService;
    private final PagoService pagoService;
    private final ReservaService reservaService;
    private final ReportesFastApiService reportesFastApiService;
    private final ExcelExportService excelExportService;
    private final boolean fastApiHabilitado;

    @Autowired
    public ReporteController(ReporteService reporteService,
                             ResidenteService residenteService,
                             ApartamentoService apartamentoService,
                             PagoService pagoService,
                             ReservaService reservaService,
                             ReportesFastApiService reportesFastApiService,
                             ExcelExportService excelExportService,
                             @Value("${app.reports.fastapi-enabled:false}") boolean fastApiHabilitado) {
        this.reporteService = reporteService;
        this.residenteService = residenteService;
        this.apartamentoService = apartamentoService;
        this.pagoService = pagoService;
        this.reservaService = reservaService;
        this.reportesFastApiService = reportesFastApiService;
        this.excelExportService = excelExportService;
        this.fastApiHabilitado = fastApiHabilitado;
    }

    public ReporteController(ReporteService reporteService,
                             ResidenteService residenteService,
                             ApartamentoService apartamentoService,
                             PagoService pagoService,
                             ReservaService reservaService,
                             ReportesFastApiService reportesFastApiService,
                             boolean fastApiHabilitado) {
        this(reporteService, residenteService, apartamentoService, pagoService, reservaService,
                reportesFastApiService, new com.nexur.nexur.service.ExcelExportService(), fastApiHabilitado);
    }

    @GetMapping("/reportes")
    public String verReportes(@RequestParam(required = false) String tipo,
                              @RequestParam(required = false) String fechaInicio,
                              @RequestParam(required = false) String fechaFin,
                              Model model) {
        LocalDate inicio = null;
        LocalDate fin = null;
        List<ReporteRegistro> resultados = List.of();
        String errorReporte = null;

        try {
            inicio = parseFecha(fechaInicio);
            fin = parseFecha(fechaFin);
        } catch (DateTimeParseException exception) {
            errorReporte = "Las fechas deben tener el formato válido AAAA-MM-DD.";
        }

        if (errorReporte == null && inicio != null && fin != null && fin.isBefore(inicio)) {
            errorReporte = "La fecha final no puede ser anterior a la fecha inicial.";
        } else if (errorReporte == null && !tipoValido(tipo)) {
            errorReporte = "El tipo de reporte seleccionado no es válido.";
        } else if (errorReporte == null) {
            resultados = reporteService.filtrarRegistros(tipo, inicio, fin);
        }

        model.addAttribute("titulo", "Reportes");
        model.addAttribute("currentPath", "/reportes");
        model.addAttribute("registros", resultados);
        model.addAttribute("tipo", tipo == null ? "TODOS" : tipo);
        model.addAttribute("fechaInicio", fechaInicio);
        model.addAttribute("fechaFin", fechaFin);
        model.addAttribute("errorReporte", errorReporte);
        model.addAttribute("residentes", residenteService.obtenerTodos().size());
        model.addAttribute("apartamentos", apartamentoService.listarApartamentos().size());
        model.addAttribute("pagosRealizados", pagoService.listarPagos().stream()
                .filter(pago -> pago.getEstadoPago() == EstadoPago.PAGADO).count());
        model.addAttribute("pagosPendientes", pagoService.listarPagos().stream()
                .filter(pago -> pago.getEstadoPago() != EstadoPago.PAGADO).count());
        model.addAttribute("reservasActivas", reservaService.listarReservas().stream()
                .filter(reserva -> reserva.getEstado() == EstadoReserva.APROBADA).count());
        model.addAttribute("fastApiReportesHabilitado", fastApiHabilitado);

        return "reportes/lista";
    }

    @GetMapping("/reportes/generar-excel")
    public ResponseEntity<byte[]> generarExcel(@RequestParam(required = false) String tipo,
                                               @RequestParam(required = false) String fechaInicio,
                                               @RequestParam(required = false) String fechaFin) {
        LocalDate inicio;
        LocalDate fin;
        try {
            inicio = parseFecha(fechaInicio);
            fin = parseFecha(fechaFin);
        } catch (DateTimeParseException exception) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Las fechas deben tener el formato válido AAAA-MM-DD.".getBytes(StandardCharsets.UTF_8));
        }
        if (inicio != null && fin != null && fin.isBefore(inicio)) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("La fecha final no puede ser anterior a la fecha inicial.".getBytes(StandardCharsets.UTF_8));
        }
        if (!tipoValido(tipo)) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("El tipo de reporte seleccionado no es válido.".getBytes(StandardCharsets.UTF_8));
        }
        byte[] excel = excelExportService.exportarReporte(
                reporteService.filtrarRegistros(tipo, inicio, fin), tipo, fechaInicio, fechaFin);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte-urbelix.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }

    @PostMapping("/reportes/generar-pdf")
    public ResponseEntity<byte[]> generarPdf(@RequestParam(required = false) String tipo,
                                             @RequestParam(required = false) String fechaInicio,
                                             @RequestParam(required = false) String fechaFin) {
        LocalDate inicio;
        LocalDate fin;
        try {
            inicio = parseFecha(fechaInicio);
            fin = parseFecha(fechaFin);
        } catch (DateTimeParseException exception) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Las fechas deben tener el formato válido AAAA-MM-DD.".getBytes(StandardCharsets.UTF_8));
        }
        if (inicio != null && fin != null && fin.isBefore(inicio)) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("La fecha final no puede ser anterior a la fecha inicial.".getBytes(StandardCharsets.UTF_8));
        }
        if (!tipoValido(tipo)) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("El tipo de reporte seleccionado no es válido.".getBytes(StandardCharsets.UTF_8));
        }
        List<ReporteRegistro> registros = reporteService.filtrarRegistros(tipo, inicio, fin);

        if (fastApiHabilitado) {
            try {
                byte[] pdf = reportesFastApiService.generarPdf(registros);
                if (pdf != null && pdf.length > 0) {
                    return respuestaPdf(pdf);
                }
                log.warn("FastAPI devolvio un PDF vacio; se usara el generador local");
            } catch (RestClientException exception) {
                log.warn("FastAPI no esta disponible; se usara el generador local", exception);
            }
        }

        return generarPdfLocal(registros);
    }

    private ResponseEntity<byte[]> generarPdfLocal(List<ReporteRegistro> registros) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        PdfDocument pdf = new PdfDocument(new PdfWriter(salida));
        Document documento = new Document(pdf);
        com.nexur.nexur.service.UrbelixPdfStyle.encabezado(documento,
            "Reporte operativo residencial",
            "Generado el " + LocalDate.now() + " | Registros: " + registros.size());
        com.itextpdf.layout.element.Table tabla = new com.itextpdf.layout.element.Table(
                new float[]{1.2f, 1.5f, 2.0f, 4.0f, 2.2f});
        tabla.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100));
        String[] encabezados = {"Tipo", "Referencia", "Residente / visitante", "Descripción", "Fecha"};
        for (String encabezado : encabezados) {
                tabla.addHeaderCell(com.nexur.nexur.service.UrbelixPdfStyle.encabezadoCelda(encabezado));
        }
        for (ReporteRegistro registro : registros) {
                tabla.addCell(com.nexur.nexur.service.UrbelixPdfStyle.celda(valor(registro.getTipo())));
                tabla.addCell(com.nexur.nexur.service.UrbelixPdfStyle.celda(valor(registro.getEntidad())));
                tabla.addCell(com.nexur.nexur.service.UrbelixPdfStyle.celda(valor(registro.getResidente())));
                tabla.addCell(com.nexur.nexur.service.UrbelixPdfStyle.celda(valor(registro.getDescripcion())));
                tabla.addCell(com.nexur.nexur.service.UrbelixPdfStyle.celda(
                    registro.getFechaHora() == null ? "" : registro.getFechaHora().toString()));
        }
        documento.add(tabla);
        documento.close();
        return respuestaPdf(salida.toByteArray());
    }

    private String valor(String texto) {
        return texto == null ? "" : texto;
    }

    private ResponseEntity<byte[]> respuestaPdf(byte[] pdf) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private LocalDate parseFecha(String fecha) {
        if (fecha == null || fecha.isBlank()) {
            return null;
        }
        return LocalDate.parse(fecha);
    }

    private boolean tipoValido(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return true;
        }
        return List.of("TODOS", "PAGOS", "RESERVAS", "VISITANTES")
                .contains(tipo.trim().toUpperCase());
    }
}
