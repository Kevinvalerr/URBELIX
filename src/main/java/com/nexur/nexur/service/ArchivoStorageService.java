package com.nexur.nexur.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ArchivoStorageService {

    private static final long MAX_BYTES = 5 * 1024 * 1024;
    private static final Set<String> TIPOS_PERMITIDOS = Set.of("application/pdf", "image/png", "image/jpeg");
    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of("pdf", "png", "jpg", "jpeg");

    private final Path directorio;

    public ArchivoStorageService(@Value("${app.upload-dir:./data/uploads}") String directorio) {
        this.directorio = Paths.get(directorio).toAbsolutePath().normalize();
    }

    public String guardar(MultipartFile archivo) {
        validar(archivo);
        String nombreInterno = UUID.randomUUID().toString();
        try {
            Files.createDirectories(directorio);
            Path destino = directorio.resolve(nombreInterno).normalize();
            if (!destino.startsWith(directorio)) {
                throw new IllegalArgumentException("Ruta de archivo no válida");
            }
            archivo.transferTo(destino);
            return nombreInterno;
        } catch (IOException exception) {
            throw new IllegalArgumentException("No se pudo guardar la evidencia", exception);
        }
    }

    public Resource cargar(String nombreInterno) {
        Path archivo = directorio.resolve(nombreInterno).normalize();
        if (!archivo.startsWith(directorio) || !Files.isRegularFile(archivo)) {
            throw new IllegalArgumentException("Evidencia no encontrada");
        }
        return new FileSystemResource(archivo);
    }

    public void eliminar(String nombreInterno) {
        try {
            Path archivo = directorio.resolve(nombreInterno).normalize();
            if (archivo.startsWith(directorio)) {
                Files.deleteIfExists(archivo);
            }
        } catch (IOException ignored) {
            // La limpieza es preventiva y no debe ocultar el error original.
        }
    }

    private void validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar una evidencia");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("La evidencia no puede superar 5 MB");
        }
        String tipo = archivo.getContentType() == null ? "" : archivo.getContentType().toLowerCase(Locale.ROOT);
        String nombre = StringUtils.hasText(archivo.getOriginalFilename()) ? archivo.getOriginalFilename() : "";
        String extension = nombre.contains(".") ? nombre.substring(nombre.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (!TIPOS_PERMITIDOS.contains(tipo) || !EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new IllegalArgumentException("Solo se permiten archivos PDF, PNG o JPG");
        }
    }
}
