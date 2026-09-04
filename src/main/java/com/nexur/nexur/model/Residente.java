package com.nexur.nexur.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import com.nexur.nexur.model.Usuario;

@Entity
@Table(name = "residentes")
public class Residente {

 @OneToOne
@JoinColumn(name = "usuario_id", unique = true)
private Usuario usuario;
 
   
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    @NotBlank(message = "Documento es obligatorio")
    @Pattern(regexp = "\\d{8,}", message = "Documento debe tener al menos 8 dígitos y solo números")
    private String documento;

    @NotBlank(message = "Teléfono es obligatorio")
    @Pattern(regexp = "\\d{10,}", message = "Teléfono debe tener al menos 10 dígitos y solo números")
    private String telefono;

    @ManyToOne
    @JoinColumn(name = "apartamento_id")
    private Apartamento apartamento;


    public Residente(){
    }

    public Residente(String nombre, String documento, String telefono, Apartamento apartamento ) {
        this.nombre = nombre;
        this.documento = documento;
        this.telefono = telefono;
        this.apartamento = apartamento;
    }

    //GETTERS Y SETTERS


    public Long getId(){
        return id;
    }

    public String getNombre() {
        return nombre;
    }

     public void setNombre(String nombre) {
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

    public void setApartamento(Apartamento apartamento) {
        this.apartamento = apartamento;
    }

    public void setId(Long id) {
    this.id = id;
}


  public Usuario getUsuario() {
    return usuario;
}

public void setUsuario(Usuario usuario) {
    this.usuario = usuario;
}


}
