package com.urbelix.urbelix.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import com.urbelix.urbelix.model.Usuario;
import java.util.ArrayList;
import java.util.List;

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

    @Email(message = "El correo debe ser válido")
    @Size(max = 255, message = "El correo no puede superar 255 caracteres")
    private String correo;

    @NotBlank(message = "Documento es obligatorio")
    @Pattern(regexp = "\\d{8,}", message = "Documento debe tener al menos 8 dígitos y solo números")
    private String documento;

    @NotBlank(message = "Teléfono es obligatorio")
    @Pattern(regexp = "\\d{10,}", message = "Teléfono debe tener al menos 10 dígitos y solo números")
    private String telefono;

    @ManyToOne
    @JoinColumn(name = "apartamento_id")
    private Apartamento apartamento;

    @OneToMany(mappedBy = "residente", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<ResidenteApartamento> asociacionesApartamento = new ArrayList<>();


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

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
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

    public List<ResidenteApartamento> getAsociacionesApartamento() {
        return asociacionesApartamento;
    }

    public void setAsociacionesApartamento(List<ResidenteApartamento> asociacionesApartamento) {
        this.asociacionesApartamento = asociacionesApartamento;
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
