package com.nexur.nexur.model.enums;

public enum TipoPago {
    ADMINISTRACION("Administración"),
    MULTA("Multa"),
    OTRO("Otro");

    private final String descripcion;

    TipoPago(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
