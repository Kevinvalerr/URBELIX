package com.nexur.nexur.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.model.Rol;
import com.nexur.nexur.model.Parqueadero;
import com.nexur.nexur.repository.ApartamentoRepository;
import com.nexur.nexur.repository.ParqueaderoRepository;
import com.nexur.nexur.repository.ResidenteRepository;
import com.nexur.nexur.repository.UsuarioRepository;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.UUID;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(UsuarioRepository usuarioRepository,
                             PasswordEncoder passwordEncoder,
                             ApartamentoRepository apartamentoRepository,
                             ResidenteRepository residenteRepository,
                             ParqueaderoRepository parqueaderoRepository,
                             JdbcTemplate jdbcTemplate,
                             Environment environment) {
        return args -> {
            boolean seedDataEnabled = Boolean.parseBoolean(
                    environment.getProperty("app.seed-data.enabled", "false"));
            if (!seedDataEnabled) {
                return;
            }

            String adminPassword = environment.getProperty("app.admin.password");
            if (adminPassword == null || adminPassword.isBlank()) {
                throw new IllegalStateException(
                        "app.admin.password debe configurarse cuando app.seed-data.enabled=true");
            }

            try {
                System.out.println("Iniciando carga de datos de prueba...");

                if (seedDataEnabled) {
                    jdbcTemplate.execute("ALTER TABLE parqueaderos ALTER COLUMN estado VARCHAR(20)");
                    jdbcTemplate.queryForList("SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS "
                                    + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'PARQUEADEROS' AND CONSTRAINT_TYPE = 'CHECK'",
                            String.class).forEach(constraint -> jdbcTemplate.execute(
                            "ALTER TABLE parqueaderos DROP CONSTRAINT \"" + constraint + "\""));
                    jdbcTemplate.queryForList("SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS "
                                    + "WHERE CONSTRAINT_SCHEMA = 'PUBLIC'").forEach(row -> {
                        String constraint = String.valueOf(row.get("CONSTRAINT_NAME"));
                        if (constraint.contains("PARQUEADERO") || constraint.contains("ESTADO")) {
                            jdbcTemplate.execute("ALTER TABLE parqueaderos DROP CONSTRAINT \"" + constraint + "\"");
                        }
                    });
                }

                // Crear usuario admin
                String adminEmail = environment.getProperty("app.admin.email", "admin@nexur.com");
                Usuario admin = usuarioRepository.findByEmail(adminEmail).orElse(null);
                boolean adminNuevo = admin == null;
                if (admin == null) {
                    admin = new Usuario();
                    admin.setNombre("Administrador");
                    admin.setEmail(adminEmail);
                    System.out.println("Usuario admin creado");
                }
                // No sobrescribir la clave de una cuenta existente en cada reinicio.
                // La recuperacion deliberada se realiza mediante AdminBootstrapRunner.
                if (adminNuevo) {
                    admin.setPassword(passwordEncoder.encode(adminPassword));
                }
                admin.setRol(Rol.ADMIN);
                // Las cuentas tecnicas de desarrollo deben permanecer disponibles localmente.
                admin.setActivo(true);
                if (adminNuevo) {
                    admin.setDebeCambiarPassword(false);
                }
                usuarioRepository.save(admin);

                String porteriaPassword = environment.getProperty("app.porteria.password");
                if (porteriaPassword != null && !porteriaPassword.isBlank()) {
                    Usuario porteria = usuarioRepository.findByEmail("porteria@nexur.com").orElseGet(Usuario::new);
                    boolean nuevo = porteria.getId() == null;
                    porteria.setNombre("Portería");
                    porteria.setEmail("porteria@nexur.com");
                    if (nuevo) {
                        porteria.setPassword(passwordEncoder.encode(porteriaPassword));
                    }
                    porteria.setRol(Rol.PORTERIA);
                    porteria.setActivo(true);
                    if (nuevo) {
                        porteria.setDebeCambiarPassword(false);
                    }
                    usuarioRepository.save(porteria);
                    System.out.println(nuevo ? "Usuario de portería creado" : "Usuario de portería actualizado");
                }

                // Crear apartamentos de prueba
                if (apartamentoRepository.count() == 0) {
                    Apartamento apto101 = new Apartamento();
                    apto101.setNumero("101");
                    apto101.setTorre("A");
                    apto101.setCodigoRegistro("URB-101-A");
                    apartamentoRepository.save(apto101);

                    Apartamento apto102 = new Apartamento();
                    apto102.setNumero("102");
                    apto102.setTorre("A");
                    apto102.setCodigoRegistro("URB-102-A");
                    apartamentoRepository.save(apto102);

                    System.out.println("Apartamentos de prueba creados");
                }

                apartamentoRepository.findAll().forEach(apartamento -> {
                    if (apartamento.getCodigoRegistro() == null || apartamento.getCodigoRegistro().isBlank()) {
                        apartamento.setCodigoRegistro("URB-" + UUID.randomUUID().toString()
                                .replace("-", "").substring(0, 12).toUpperCase());
                        apartamentoRepository.save(apartamento);
                    }
                });

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

                if (parqueaderoRepository.count() == 0) {
                    Parqueadero parqueadero1 = new Parqueadero();
                    parqueadero1.setNumero("P-01");
                    parqueadero1.setZona("Sotano 1");
                    parqueaderoRepository.save(parqueadero1);

                    Parqueadero parqueadero2 = new Parqueadero();
                    parqueadero2.setNumero("P-02");
                    parqueadero2.setZona("Sotano 1");
                    parqueaderoRepository.save(parqueadero2);

                    Parqueadero parqueadero3 = new Parqueadero();
                    parqueadero3.setNumero("P-03");
                    parqueadero3.setZona("Sotano 2");
                    parqueaderoRepository.save(parqueadero3);
                    System.out.println("Parqueaderos de prueba creados");
                }

                parqueaderoRepository.findAll().forEach(parqueadero -> {
                    if (parqueadero.getTipo() == null) {
                        parqueadero.setTipo(com.nexur.nexur.model.TipoVehiculo.CARRO);
                        parqueaderoRepository.save(parqueadero);
                    }
                });

                System.out.println("Carga de datos completada exitosamente");

            } catch (Exception e) {
                System.err.println("Error en carga de datos: " + e.getMessage());
                throw new IllegalStateException(
                        "No se pudo completar la carga de datos de desarrollo", e);
            }
        };
    }
}
