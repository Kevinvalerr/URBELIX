package com.nexur.nexur.model;

import java.time.LocalDateTime;

public class ReporteRegistro {

    private String tipo;
    private String entidad;
    private String usuario;
    private String descripcion;
    private LocalDateTime fechaHora;

    public ReporteRegistro(String tipo, String entidad, String usuario, String descripcion, LocalDateTime fechaHora) {
        this.tipo = tipo;
        this.entidad = entidad;
        this.usuario = usuario;
        this.descripcion = descripcion;
        this.fechaHora = fechaHora;
    }

    public String getTipo() {
        return tipo;
    }

    public String getEntidad() {
        return entidad;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }
}
