package com.nexur.nexur.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del residente es obligatorio")
    private String residente;

    @NotBlank(message = "El monto es obligatorio")
    private String monto;

    @NotBlank(message = "El método de pago es obligatorio")
    private String metodo;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    private LocalDateTime creadoEn;

    @ManyToOne
    @JoinColumn(name = "apartamento_id")
    private Apartamento apartamento;

    public Pago() {
        this.creadoEn = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getResidente() {
        return residente;
    }

    public void setResidente(String residente) {
        this.residente = residente;
    }

    public String getMonto() {
        return monto;
    }

    public void setMonto(String monto) {
        this.monto = monto;
    }

    public String getMetodo() {
        return metodo;
    }

    public void setMetodo(String metodo) {
        this.metodo = metodo;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }

    public Apartamento getApartamento() {
        return apartamento;
    }

    public void setApartamento(Apartamento apartamento) {
        this.apartamento = apartamento;
    }
}
