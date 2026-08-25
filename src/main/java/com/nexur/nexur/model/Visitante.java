package com.nexur.nexur.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "visitantes")
public class Visitante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nombre del visitante es obligatorio")
    private String nombre;

    @NotBlank(message = "Documento del visitante es obligatorio")
    @Pattern(regexp = "\\d{8,}", message = "Documento debe tener al menos 8 dígitos y solo números")
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
