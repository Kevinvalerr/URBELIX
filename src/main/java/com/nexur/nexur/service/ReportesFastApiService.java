package com.nexur.nexur.service;

import com.nexur.nexur.model.ReporteRegistro;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class ReportesFastApiService {

    private final RestClient client;

    public ReportesFastApiService(@Value("${urbelix.fastapi.url:http://127.0.0.1:8000}") String url) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(15000);
        this.client = RestClient.builder()
                .baseUrl(url)
                .requestFactory(requestFactory)
                .build();
    }

    public byte[] generarPdf(List<ReporteRegistro> registros) {
        return client.post()
                .uri("/reportes/generar-pdf")
                .contentType(MediaType.APPLICATION_JSON)
                .body(registros)
                .retrieve()
                .body(byte[].class);
    }
}
