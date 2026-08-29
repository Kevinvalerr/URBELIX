package com.nexur.nexur.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "parqueaderos")
public class Parqueadero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El número del parqueadero es obligatorio")
    @Column(nullable = false, unique = true)
    private String numero;

    private String zona;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoParqueadero estado = EstadoParqueadero.DISPONIBLE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'CARRO'")
    private TipoVehiculo tipo = TipoVehiculo.CARRO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartamento_id")
    private Apartamento apartamento;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehiculo_id", unique = true)
    private Vehiculo vehiculo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getZona() {
        return zona;
    }

    public void setZona(String zona) {
        this.zona = zona;
    }

    public EstadoParqueadero getEstado() {
        return estado;
    }

    public void setEstado(EstadoParqueadero estado) {
        this.estado = estado;
    }

    public Apartamento getApartamento() {
        return apartamento;
    }

    public void setApartamento(Apartamento apartamento) {
        this.apartamento = apartamento;
    }

    public TipoVehiculo getTipo() {
        return tipo;
    }

    public void setTipo(TipoVehiculo tipo) {
        this.tipo = tipo;
    }

    public Vehiculo getVehiculo() {
        return vehiculo;
    }

    public void setVehiculo(Vehiculo vehiculo) {
        this.vehiculo = vehiculo;
    }
}
