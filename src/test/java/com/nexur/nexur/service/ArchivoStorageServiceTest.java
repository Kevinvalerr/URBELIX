package com.nexur.nexur.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchivoStorageServiceTest {

    @TempDir
    Path directorioTemporal;

    @Test
    void guardaYRecuperaUnaEvidenciaValida() throws IOException {
        ArchivoStorageService service = new ArchivoStorageService(directorioTemporal.toString());
        byte[] contenido = "evidencia de prueba".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "evidencia.pdf", "application/pdf", contenido);

        String nombreInterno = service.guardar(archivo);
        Resource recurso = service.cargar(nombreInterno);

        assertTrue(recurso.exists());
        assertArrayEquals(contenido, recurso.getInputStream().readAllBytes());
        assertTrue(Files.exists(directorioTemporal.resolve(nombreInterno)));
    }

    @Test
    void rechazaExtensionYMimeNoPermitidos() {
        ArchivoStorageService service = new ArchivoStorageService(directorioTemporal.toString());
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "evidencia.exe", "application/octet-stream", new byte[]{1, 2, 3});

        assertThrows(IllegalArgumentException.class, () -> service.guardar(archivo));
    }

    @Test
    void rechazaRutasDeDescargaFueraDelDirectorio() {
        ArchivoStorageService service = new ArchivoStorageService(directorioTemporal.toString());

        assertThrows(IllegalArgumentException.class, () -> service.cargar("..\\secreto.txt"));
    }
}
