package com.nexur.nexur.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "incidencia")
@Getter
@Setter
@NoArgsConstructor
public class Incidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El asunto es obligatorio")
    @Column(nullable = false, length = 120)
    private String asunto;

    @NotBlank(message = "La descripción es obligatoria")
    @Column(nullable = false, length = 2000)
    private String descripcion;

    @Column(nullable = false, length = 30)
    private String tipo = "GENERAL";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoIncidencia estado = EstadoIncidencia.ABIERTA;

    @Column(length = 2000)
    private String respuesta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "residente_id", nullable = false)
    private Residente residente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartamento_id")
    private Apartamento apartamento;

    @Column(nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    private LocalDateTime actualizadoEn;

    @OneToMany(mappedBy = "incidencia", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<IncidenciaComentario> comentarios = new ArrayList<>();

    @OneToMany(mappedBy = "incidencia", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<IncidenciaAdjunto> adjuntos = new ArrayList<>();
}
