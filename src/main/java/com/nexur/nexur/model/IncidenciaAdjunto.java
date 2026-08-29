package com.nexur.nexur.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "incidencia_adjunto")
@Getter
@Setter
@NoArgsConstructor
public class IncidenciaAdjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String nombreOriginal;

    @Column(nullable = false, unique = true, length = 80)
    private String nombreInterno;

    @Column(nullable = false, length = 100)
    private String tipoContenido;

    @Column(nullable = false)
    private long tamano;

    @Column(nullable = false, length = 180)
    private String cargadoPor;

    @Column(nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incidencia_id", nullable = false)
    private Incidencia incidencia;
}
