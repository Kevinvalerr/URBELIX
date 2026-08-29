package com.nexur.nexur.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "avisos")
@Getter
@Setter
@NoArgsConstructor
public class Aviso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El título es obligatorio")
    @Column(nullable = false, length = 120)
    private String titulo;

    @NotBlank(message = "El contenido es obligatorio")
    @Column(nullable = false, length = 2000)
    private String contenido;

    @Column(nullable = false)
    private LocalDateTime publicadoEn = LocalDateTime.now();

    private LocalDateTime venceEn;

    @Column(nullable = false)
    private boolean activo = true;
}
