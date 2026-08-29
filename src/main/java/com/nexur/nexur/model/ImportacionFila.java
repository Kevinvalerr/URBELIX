package com.nexur.nexur.model;

public class ImportacionFila {

    private final int fila;
    private final String documento;
    private final String nombre;
    private final String estado;
    private final String motivo;

    public ImportacionFila(int fila, String documento, String nombre, String estado, String motivo) {
        this.fila = fila;
        this.documento = documento;
        this.nombre = nombre;
        this.estado = estado;
        this.motivo = motivo;
    }

    public int getFila() {
        return fila;
    }

    public String getDocumento() {
        return documento;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEstado() {
        return estado;
    }

    public String getMotivo() {
        return motivo;
    }
}
