package com.nexur.nexur.model;

import java.util.ArrayList;
import java.util.List;

public class ImportacionResultado {

    private int total;
    private int creados;
    private int omitidos;
    private int duplicados;
    private int errores;
    private final List<ImportacionFila> filas = new ArrayList<>();

    public void incrementarTotal() {
        total++;
    }

    public void creado(ImportacionFila fila) {
        creados++;
        filas.add(fila);
    }

    public void omitido(ImportacionFila fila) {
        omitidos++;
        filas.add(fila);
    }

    public void duplicado(ImportacionFila fila) {
        duplicados++;
        filas.add(fila);
    }

    public void error(ImportacionFila fila) {
        errores++;
        filas.add(fila);
    }

    public int getTotal() {
        return total;
    }

    public int getCreados() {
        return creados;
    }

    public int getOmitidos() {
        return omitidos;
    }

    public int getDuplicados() {
        return duplicados;
    }

    public int getErrores() {
        return errores;
    }

    public List<ImportacionFila> getFilas() {
        return filas;
    }
}
