package com.nexur.nexur.model;

import jakarta.persistence.*;
import lombok.*;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String email;
    private String password;
   
    @Enumerated(EnumType.STRING)
    private Rol rol;
    
    public Rol getRol(){
        return rol;
    }

    public void setRol(Rol rol){
        this.rol = rol;
    }
    
}
