package com.urbelix.urbelix.service;

import com.urbelix.urbelix.model.Incidencia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.util.Map;

@Service
public class IncidenciaFastApiService {
    private static final Logger log = LoggerFactory.getLogger(IncidenciaFastApiService.class);
    private final RestClient client;

    public IncidenciaFastApiService(@Value("${urbelix.fastapi.url:http://localhost:8000}") String url) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);
        client = RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }

    public boolean analizar(Incidencia incidencia) {
        try {
            client.post().uri("/incidencias/analizar").body(Map.of(
                    "id", incidencia.getId(), "titulo", incidencia.getTitulo(),
                    "descripcion", incidencia.getDescripcion(), "prioridad", incidencia.getPrioridad().name(),
                    "estado", incidencia.getEstado().name())).retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException ex) {
            log.warn("FastAPI no disponible para la incidencia {}", incidencia.getId());
            return false;
        }
    }
}
