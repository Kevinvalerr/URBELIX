package com.nexur.nexur.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.nexur.nexur.model.enums.EstadoReserva;
import com.nexur.nexur.model.enums.TipoEspacio;

@Entity
@Table(name = "reservas")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

  
    
   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "residente_id", nullable = true)
   private Residente residente;

    @NotNull(message = "El área de reserva es obligatoria")
    @Enumerated(EnumType.STRING)
    private TipoEspacio tipoEspacio;

    @NotNull(message = "La fecha de reserva es obligatoria")
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;

    private String observaciones;

   @Enumerated(EnumType.STRING)
   private EstadoReserva estado;

    private LocalDateTime creadoEn;

    @ManyToOne
    @JoinColumn(name = "apartamento_id")
    private Apartamento apartamento;

    public Reserva() {
        this.estado = EstadoReserva.PENDIENTE;
        this.creadoEn = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Residente getResidente() {
        return residente;
    }

    public void setResidente(Residente residente) {
    this.residente = residente;
}

    public TipoEspacio getTipoEspacio() {
        return tipoEspacio;
    }

    public void setTipoEspacio(TipoEspacio tipoEspacio) {
        this.tipoEspacio = tipoEspacio;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;

    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public void setEstado(EstadoReserva estado) {
        this.estado = estado;
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
