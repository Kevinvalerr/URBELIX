package com.nexur.nexur.model;

import jakarta.persistence.*;

@Entity
@Table(name = "residentes")
public class Residente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    private String documento;

    private String telefono;
  
    // Relación con Apartamento
    @ManyToOne
    @JoinColumn(name = "apartamento:id")
    private Apartamento apartamento;

    public Residente(){
    }

    public Residente(String nombre, String documento, String telefono, Apartamento apartamento) {
       this.nombre = nombre;
       this.documento = documento;
       this.telefono = telefono;
       this.apartamento = apartamento; 
    }

    // GETTERS Y SETTERS

    public Long getId() {
        return id;
    }

    public String getNombre(){
        return nombre;
    }

    public void setNombre(String nombre){
        this.nombre = nombre;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public String getTelefono() {
        return telefono;
    }

   public void setTelefono(String telefono) {
    this.telefono = telefono;
   }

   public Apartamento getApartamento() {
     return apartamento;
   }

   public void setApartamento(Apartamento apartamento){
     this.apartamento = apartamento;
   }
}

