package com.urbelix.urbelix.model;

import com.urbelix.urbelix.model.enums.EstadoIncidencia;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidencia_historial", indexes = @Index(name = "idx_historial_incidencia", columnList = "incidencia_id"))
public class IncidenciaHistorial {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "incidencia_id", nullable = false) private Incidencia incidencia;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "actor_usuario_id") private Usuario actor;
    @Enumerated(EnumType.STRING) @Column(name = "estado_anterior", length = 20) private EstadoIncidencia estadoAnterior;
    @Enumerated(EnumType.STRING) @Column(name = "estado_nuevo", nullable = false, length = 20) private EstadoIncidencia estadoNuevo;
    @Column(length = 2000) private String comentario;
    @Column(nullable = false) private LocalDateTime fecha;
    protected IncidenciaHistorial() { }
    public IncidenciaHistorial(Incidencia incidencia, Usuario actor, EstadoIncidencia anterior, EstadoIncidencia nuevo, String comentario) { this.incidencia = incidencia; this.actor = actor; this.estadoAnterior = anterior; this.estadoNuevo = nuevo; this.comentario = comentario; this.fecha = LocalDateTime.now(); }
    public Long getId() { return id; }
    public Usuario getActor() { return actor; }
    public EstadoIncidencia getEstadoAnterior() { return estadoAnterior; }
    public EstadoIncidencia getEstadoNuevo() { return estadoNuevo; }
    public String getComentario() { return comentario; }
    public LocalDateTime getFecha() { return fecha; }
}
