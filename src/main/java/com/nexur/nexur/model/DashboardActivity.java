package com.nexur.nexur.model;

import java.time.LocalDateTime;

public class DashboardActivity {
    private String usuario;
    private String accion;
    private String fecha;
    private String tipo;
    private LocalDateTime fechaHora;

    public DashboardActivity(String usuario, String accion, String fecha, String tipo, LocalDateTime fechaHora) {
        this.usuario = usuario;
        this.accion = accion;
        this.fecha = fecha;
        this.tipo = tipo;
        this.fechaHora = fechaHora;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getAccion() {
        return accion;
    }

    public String getFecha() {
        return fecha;
    }

    public String getTipo() {
        return tipo;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }
}
