package com.nexur.nexur.model.enums;

public enum MetodoPago {
    PSE("PSE"),
    TARJETA("Tarjeta"),
    EFECTIVO("Efectivo"),
    TRANSFERENCIA("Transferencia");

    private final String descripcion;

    MetodoPago(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}