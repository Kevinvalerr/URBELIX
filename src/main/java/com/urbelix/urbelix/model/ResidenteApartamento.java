package com.urbelix.urbelix.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "residente_apartamento", uniqueConstraints = {
        @UniqueConstraint(name = "uk_residente_apartamento_activo", columnNames = {
                "residente_id", "apartamento_id", "activo"
        })
})
public class ResidenteApartamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "residente_id", nullable = false)
    private Residente residente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "apartamento_id", nullable = false)
    private Apartamento apartamento;

    private LocalDate fechaAsignacion;
    private LocalDate fechaFin;

    @Column(nullable = false)
    private boolean activo = true;

    public Long getId() {
        return id;
    }

    public Residente getResidente() {
        return residente;
    }

    public void setResidente(Residente residente) {
        this.residente = residente;
    }

    public Apartamento getApartamento() {
        return apartamento;
    }

    public void setApartamento(Apartamento apartamento) {
        this.apartamento = apartamento;
    }

    public LocalDate getFechaAsignacion() {
        return fechaAsignacion;
    }

    public void setFechaAsignacion(LocalDate fechaAsignacion) {
        this.fechaAsignacion = fechaAsignacion;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
