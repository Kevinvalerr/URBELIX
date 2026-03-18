package com.nexur.nexur.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "visitantes")
public class Visitante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    private String documento;

    private LocalDateTime fechaEntrada;

    private LocalDateTime fechaSalida;

    @ManyToOne
    @JoinColumn(name = "apartamento_id")
    private Apartamento apartamento;
    
    public Visitante() {}

    public Long getId() {
       return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDocumento() {
        return documento;
    
    }

    public void setDocumento(String documento ) {
        this.documento = documento;

    }

    public  LocalDateTime getFechaEntrada() {
        return fechaEntrada;

    }

    public void setFechaEntrada(LocalDateTime  fechaEntrada) {
        this.fechaEntrada = fechaEntrada;

    }

    public LocalDateTime getFechaSalida() {
        return fechaSalida;

    }

    public void setFechaSalida(LocalDateTime fechaSalida) {
        this.fechaSalida = fechaSalida;

    }

    public Apartamento getApartamento() {
        return apartamento;

    }

    public void setApartamento(Apartamento aprtamento) {
        this.apartamento = aprtamento;

    }

    
}
