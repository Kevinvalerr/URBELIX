package com.urbelix.urbelix.model;

import com.urbelix.urbelix.model.enums.EstadoIncidencia;
import com.urbelix.urbelix.model.enums.PrioridadIncidencia;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "incidencias", indexes = {
        @Index(name = "idx_incidencia_estado", columnList = "estado"),
        @Index(name = "idx_incidencia_prioridad", columnList = "prioridad"),
        @Index(name = "idx_incidencia_fecha", columnList = "fecha_creacion")
})
public class Incidencia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "El título es obligatorio")
    @Size(max = 150, message = "El título no puede superar 150 caracteres")
    @Column(nullable = false, length = 150) private String titulo;
    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
    @Column(nullable = false, length = 2000) private String descripcion;
    @Size(max = 80, message = "La categoría no puede superar 80 caracteres")
    @Column(length = 80) private String categoria;
    @NotNull(message = "La prioridad es obligatoria")
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PrioridadIncidencia prioridad = PrioridadIncidencia.MEDIA;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private EstadoIncidencia estado = EstadoIncidencia.PENDIENTE;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "residente_id", nullable = false) private Residente residente;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "apartamento_id") private Apartamento apartamento;
    @Column(name = "motivo_rechazo", length = 1000) private String motivoRechazo;
    @Column(name = "observacion_resolucion", length = 1000) private String observacionResolucion;
    @Column(name = "fecha_creacion", nullable = false) private LocalDateTime fechaCreacion;
    @Column(name = "fecha_actualizacion", nullable = false) private LocalDateTime fechaActualizacion;
    @OneToMany(mappedBy = "incidencia", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fecha ASC") private List<IncidenciaHistorial> historial = new ArrayList<>();

    @PrePersist void crearFechas() { fechaCreacion = LocalDateTime.now(); fechaActualizacion = fechaCreacion; }
    @PreUpdate void actualizarFecha() { fechaActualizacion = LocalDateTime.now(); }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public PrioridadIncidencia getPrioridad() { return prioridad; }
    public void setPrioridad(PrioridadIncidencia prioridad) { this.prioridad = prioridad; }
    public EstadoIncidencia getEstado() { return estado; }
    public void setEstado(EstadoIncidencia estado) { this.estado = estado; }
    public Residente getResidente() { return residente; }
    public void setResidente(Residente residente) { this.residente = residente; }
    public Apartamento getApartamento() { return apartamento; }
    public void setApartamento(Apartamento apartamento) { this.apartamento = apartamento; }
    public String getMotivoRechazo() { return motivoRechazo; }
    public void setMotivoRechazo(String motivoRechazo) { this.motivoRechazo = motivoRechazo; }
    public String getObservacionResolucion() { return observacionResolucion; }
    public void setObservacionResolucion(String observacionResolucion) { this.observacionResolucion = observacionResolucion; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public List<IncidenciaHistorial> getHistorial() { return historial; }
}
