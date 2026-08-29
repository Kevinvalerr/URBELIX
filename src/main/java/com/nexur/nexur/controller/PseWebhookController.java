package com.nexur.nexur.controller;

import com.nexur.nexur.service.PseWebhookService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

@RestController
@RequestMapping("/webhooks/pagos")
public class PseWebhookController {

    private final PseWebhookService pseWebhookService;
    private final JsonMapper jsonMapper;

    public PseWebhookController(PseWebhookService pseWebhookService, JsonMapper jsonMapper) {
        this.pseWebhookService = pseWebhookService;
        this.jsonMapper = jsonMapper;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> recibir(
            @RequestHeader(value = "X-PSE-Signature", required = false) String firma,
            @RequestBody String cuerpo) {
        if (!pseWebhookService.firmaValida(cuerpo, firma)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Firma invalida"));
        }
        try {
            PseWebhookService.PseWebhookRequest solicitud = jsonMapper.readValue(
                    cuerpo, PseWebhookService.PseWebhookRequest.class);
            PseWebhookService.Resultado resultado = pseWebhookService.procesar(solicitud);
            return ResponseEntity.ok(Map.of("resultado", resultado.name()));
        } catch (JacksonException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", "JSON invalido"));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("error", exception.getMessage()));
        }
    }
}
