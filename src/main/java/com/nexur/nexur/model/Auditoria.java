package com.nexur.nexur.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria", indexes = {
        @Index(name = "idx_auditoria_fecha", columnList = "creado_en"),
        @Index(name = "idx_auditoria_actor", columnList = "actor_email, creado_en")
})
@Getter
@Setter
@NoArgsConstructor
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_email", nullable = false, length = 180)
    private String actorEmail;

    @Column(nullable = false, length = 120)
    private String accion;

    @Column(nullable = false, length = 255)
    private String entidad;

    @Column(name = "entidad_id")
    private Long entidadId;

    @Column(length = 500)
    private String detalle;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
