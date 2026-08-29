package com.nexur.nexur.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Entity
@Table(name = "vehiculos")
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "La placa es obligatoria")
    @Pattern(regexp = "[A-Za-z0-9-]{3,10}", message = "La placa debe tener entre 3 y 10 caracteres")
    @Column(nullable = false, unique = true, length = 10)
    private String placa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoVehiculo tipo;

    private String marca;
    private String modelo;
    private String color;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "residente_id", nullable = false)
    private Residente residente;

    @OneToOne(mappedBy = "vehiculo", fetch = FetchType.LAZY)
    private Parqueadero parqueadero;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }
    public TipoVehiculo getTipo() { return tipo; }
    public void setTipo(TipoVehiculo tipo) { this.tipo = tipo; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public Residente getResidente() { return residente; }
    public void setResidente(Residente residente) { this.residente = residente; }
    public Parqueadero getParqueadero() { return parqueadero; }
    public void setParqueadero(Parqueadero parqueadero) { this.parqueadero = parqueadero; }
}
