package com.nexur.nexur.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.model.Rol;
import com.nexur.nexur.repository.ApartamentoRepository;
import com.nexur.nexur.repository.ResidenteRepository;
import com.nexur.nexur.repository.UsuarioRepository;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(UsuarioRepository usuarioRepository,
                             PasswordEncoder passwordEncoder,
                             ApartamentoRepository apartamentoRepository,
                             ResidenteRepository residenteRepository) {
        return args -> {
            try {
                System.out.println("Iniciando carga de datos de prueba...");

                // Crear usuario admin
                if (usuarioRepository.findByEmail("admin@nexur.com").isEmpty()) {
                    Usuario admin = new Usuario();
                    admin.setNombre("Administrador");
                    admin.setEmail("admin@nexur.com");
                    admin.setPassword(passwordEncoder.encode("admin123"));
                    admin.setRol(Rol.ADMIN);
                    usuarioRepository.save(admin);
                    System.out.println("Usuario admin creado");
                }

                // Crear apartamentos de prueba
                if (apartamentoRepository.count() == 0) {
                    Apartamento apto101 = new Apartamento();
                    apto101.setNumero("101");
                    apto101.setTorre("A");
                    apartamentoRepository.save(apto101);

                    Apartamento apto102 = new Apartamento();
                    apto102.setNumero("102");
                    apto102.setTorre("A");
                    apartamentoRepository.save(apto102);

                    System.out.println("Apartamentos de prueba creados");
                }

                // Crear residentes de prueba
                if (residenteRepository.count() == 0) {
                    List<Apartamento> apartamentos = apartamentoRepository.findAll();

                    if (!apartamentos.isEmpty()) {
                        Residente residente1 = new Residente();
                        residente1.setNombre("Maria Garcia");
                        residente1.setDocumento("87654321");
                        residente1.setTelefono("3007654321");
                        residente1.setApartamento(apartamentos.get(0));
                        residenteRepository.save(residente1);

                        Residente residente2 = new Residente();
                        residente2.setNombre("Carlos Rodriguez");
                        residente2.setDocumento("11223344");
                        residente2.setTelefono("3001122334");
                        residente2.setApartamento(apartamentos.get(1));
                        residenteRepository.save(residente2);

                        System.out.println("Residentes de prueba creados");
                    }
                }

                System.out.println("Carga de datos completada exitosamente");

            } catch (Exception e) {
                System.err.println("Error en carga de datos: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }
}
