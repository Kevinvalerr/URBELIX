package com.nexur.nexur.model;

import jakarta.persistence.*;

@Entity

@Table(name = "apartamentos")
public class Apartamento {

    @Id

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String numero;

    private String torre;

    private Integer piso;

    private String  estado;

    public Apartamento() {   
    }

    public Apartamento(String numero, String torre, Integer piso, String estado){
        this.numero = numero;
        this.torre = torre;
        this.piso = piso;
        this.estado = estado;

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumero(){
        return numero;
    }

    public void setNumero(String numero){
        this.numero = numero;
    }

    public String getTorre() {
        return torre;
    }

    public void setTorre(String torre){
        this.torre = torre;
    }

    public Integer getPiso() {
        return piso;
    }

    public void setPiso(Integer piso){
        this.piso = piso;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado){
        this.estado = estado;
    }
}
