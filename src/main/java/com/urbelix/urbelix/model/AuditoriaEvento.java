package com.urbelix.urbelix.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_eventos", indexes = {
        @Index(name = "idx_auditoria_fecha", columnList = "fecha_hora"),
        @Index(name = "idx_auditoria_actor", columnList = "actor_usuario_id")
})
public class AuditoriaEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_usuario_id")
    private Usuario actor;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false, length = 80)
    private String accion;

    @Column(nullable = false, length = 80)
    private String modulo;

    @Column(length = 80)
    private String entidad;

    @Column(name = "entidad_id")
    private Long entidadId;

    @Column(nullable = false, length = 20)
    private String resultado;

    @Column(length = 1000)
    private String descripcion;

    protected AuditoriaEvento() {
    }

    public AuditoriaEvento(Usuario actor, String accion, String modulo, String entidad,
                           Long entidadId, String resultado, String descripcion) {
        this.actor = actor;
        this.fechaHora = LocalDateTime.now();
        this.accion = accion;
        this.modulo = modulo;
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.resultado = resultado;
        this.descripcion = descripcion;
    }

    public Long getId() { return id; }
    public Usuario getActor() { return actor; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public String getAccion() { return accion; }
    public String getModulo() { return modulo; }
    public String getEntidad() { return entidad; }
    public Long getEntidadId() { return entidadId; }
    public String getResultado() { return resultado; }
    public String getDescripcion() { return descripcion; }
}
