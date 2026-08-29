package com.nexur.nexur.controller;

import com.nexur.nexur.service.WompiService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/webhooks/wompi")
public class WompiWebhookController {

    private final WompiService wompiService;

    public WompiWebhookController(WompiService wompiService) {
        this.wompiService = wompiService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> recibir(
            @RequestHeader(value = "X-Event-Checksum", required = false) String checksum,
            @RequestBody String cuerpo) {
        try {
            WompiService.Resultado resultado = wompiService.procesarEvento(cuerpo, checksum);
            return ResponseEntity.ok(Map.of("resultado", resultado.name()));
        } catch (WompiService.FirmaInvalidaException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Firma Wompi invalida"));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("error", exception.getMessage()));
        }
    }
}
