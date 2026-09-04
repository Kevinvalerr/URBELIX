package com.nexur.nexur.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'PENDIENTE'")
    private EstadoVisitante estado = EstadoVisitante.PENDIENTE;

    @Column(length = 500)
    private String motivoRechazo;

    @ManyToOne
    @JoinColumn(name = "apartamento_id")
    private Apartamento apartamento;

    public Visitante() {}

    public Long getId() {
       return id;
    }

    public void setId(Long id) {
       this.id = id;
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

    public EstadoVisitante getEstado() {
        return estado;
    }

    public void setEstado(EstadoVisitante estado) {
        this.estado = estado;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public Apartamento getApartamento() {
        return apartamento;

    }

    public void setApartamento(Apartamento aprtamento) {
        this.apartamento = aprtamento;

    }


}
