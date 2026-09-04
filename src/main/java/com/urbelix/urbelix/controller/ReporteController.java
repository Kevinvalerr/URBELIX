package com.urbelix.urbelix.controller;

import com.urbelix.urbelix.model.ReporteRegistro;
import com.urbelix.urbelix.service.ReporteService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class ReporteController {

    private final ReporteService reporteService;
    private final RestClient fastApiClient;

    public ReporteController(ReporteService reporteService,
                             @Value("${urbelix.fastapi.url:http://localhost:8000}") String fastApiUrl) {
        this.reporteService = reporteService;
                    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
                    requestFactory.setConnectTimeout(3000);
                    requestFactory.setReadTimeout(15000);
                    this.fastApiClient = RestClient.builder()
                        .baseUrl(fastApiUrl)
                        .requestFactory(requestFactory)
                        .build();
    }

    @GetMapping("/reportes")
    public String verReportes(@RequestParam(required = false) String tipo,
                              @RequestParam(required = false) String fechaInicio,
                              @RequestParam(required = false) String fechaFin,
                              Model model) {
        LocalDate inicio = fechaInicio != null && !fechaInicio.isBlank() ? LocalDate.parse(fechaInicio) : null;
        LocalDate fin = fechaFin != null && !fechaFin.isBlank() ? LocalDate.parse(fechaFin) : null;

        List<ReporteRegistro> resultados = reporteService.filtrarRegistros(tipo, inicio, fin);

        model.addAttribute("titulo", "Reportes");
        model.addAttribute("currentPath", "/reportes");
        model.addAttribute("registros", resultados);
        model.addAttribute("tipo", tipo == null ? "TODOS" : tipo);
        model.addAttribute("fechaInicio", fechaInicio);
        model.addAttribute("fechaFin", fechaFin);

        return "reportes/lista";
    }

    @PostMapping("/reportes/generar-pdf")
    public ResponseEntity<byte[]> generarReportePdf() {
        List<ReporteRegistro> registros = reporteService.filtrarRegistros("TODOS", null, null);
        try {
            byte[] pdf = fastApiClient.post()
                    .uri("/reportes/generar-pdf")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(registros)
                    .retrieve()
                    .body(byte[].class);

            if (pdf == null || pdf.length == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "FastAPI devolvió un PDF vacío");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("reporte_urbelix_" + LocalDate.now() + ".pdf").build());
            return ResponseEntity.ok().headers(headers).body(pdf);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El servicio FastAPI no está disponible. Inícialo en http://localhost:8000", ex);
        }
    }
}
