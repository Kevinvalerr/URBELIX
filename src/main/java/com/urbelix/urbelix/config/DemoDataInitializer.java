package com.urbelix.urbelix.config;

import com.urbelix.urbelix.model.Apartamento;
import com.urbelix.urbelix.model.AuditoriaEvento;
import com.urbelix.urbelix.model.Incidencia;
import com.urbelix.urbelix.model.IncidenciaHistorial;
import com.urbelix.urbelix.model.Pago;
import com.urbelix.urbelix.model.Reserva;
import com.urbelix.urbelix.model.Residente;
import com.urbelix.urbelix.model.ResidenteApartamento;
import com.urbelix.urbelix.model.Rol;
import com.urbelix.urbelix.model.Usuario;
import com.urbelix.urbelix.model.Visitante;
import com.urbelix.urbelix.model.enums.EstadoIncidencia;
import com.urbelix.urbelix.model.enums.EstadoPago;
import com.urbelix.urbelix.model.enums.EstadoReserva;
import com.urbelix.urbelix.model.enums.MetodoPago;
import com.urbelix.urbelix.model.enums.PrioridadIncidencia;
import com.urbelix.urbelix.model.enums.TipoEspacio;
import com.urbelix.urbelix.model.enums.TipoPago;
import com.urbelix.urbelix.repository.ApartamentoRepository;
import com.urbelix.urbelix.repository.AuditoriaEventoRepository;
import com.urbelix.urbelix.repository.IncidenciaRepository;
import com.urbelix.urbelix.repository.PagoRepository;
import com.urbelix.urbelix.repository.ReservaRepository;
import com.urbelix.urbelix.repository.ResidenteApartamentoRepository;
import com.urbelix.urbelix.repository.ResidenteRepository;
import com.urbelix.urbelix.repository.UsuarioRepository;
import com.urbelix.urbelix.repository.VisitanteRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Carga un conjunto de datos de demostracion coherente: un conjunto residencial
 * completo con apartamentos, residentes, pagos en distintos estados, reservas,
 * visitantes e incidencias con su historial.
 *
 * Solo se activa con el perfil "demo", que se combina con el de despliegue:
 *   SPRING_PROFILES_ACTIVE=prod,demo
 *
 * Es idempotente: si ya existen apartamentos no vuelve a sembrar, de modo que
 * los datos que se creen desde la interfaz durante la demo sobreviven a un
 * reinicio del contenedor.
 */
@Configuration
@Profile("demo")
public class DemoDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private static final String DOMINIO = "@urbelix.demo";

    @Value("${urbelix.demo.admin-email:admin" + DOMINIO + "}")
    private String adminEmail;

    @Value("${urbelix.demo.admin-password:}")
    private String adminPassword;

    @Value("${urbelix.demo.residente-password:}")
    private String residentePassword;

    @Bean
    CommandLineRunner cargarDatosDemo(UsuarioRepository usuarios,
                                      ApartamentoRepository apartamentos,
                                      ResidenteRepository residentes,
                                      ResidenteApartamentoRepository asociaciones,
                                      PagoRepository pagos,
                                      ReservaRepository reservas,
                                      VisitanteRepository visitantes,
                                      IncidenciaRepository incidencias,
                                      AuditoriaEventoRepository auditoria,
                                      PasswordEncoder encoder) {
        return args -> {
            if (apartamentos.count() > 0) {
                log.info("Perfil demo: ya hay datos cargados, no se siembra nada.");
                return;
            }
            if (adminPassword == null || adminPassword.isBlank()
                    || residentePassword == null || residentePassword.isBlank()) {
                throw new IllegalStateException("""
                        Perfil demo activo sin contrasenas configuradas. \
                        Define DEMO_ADMIN_PASSWORD y DEMO_RESIDENTE_PASSWORD \
                        antes de arrancar.""");
            }

            log.info("Perfil demo: sembrando conjunto residencial de demostracion...");

            var admin = crearUsuario(usuarios, encoder, "Laura Restrepo",
                    adminEmail, adminPassword, Rol.ADMIN);
            var porteria = crearUsuario(usuarios, encoder, "Recepcion Torre A",
                    "porteria" + DOMINIO, residentePassword, Rol.PORTERIA);

            List<Apartamento> aptos = crearApartamentos(apartamentos);
            List<Residente> vecinos = crearResidentes(residentes, asociaciones, usuarios,
                    encoder, aptos);

            crearPagos(pagos, vecinos);
            crearReservas(reservas, vecinos);
            crearVisitantes(visitantes, aptos);
            crearIncidencias(incidencias, vecinos, admin);
            crearAuditoria(auditoria, admin, porteria);

            log.info("Perfil demo: {} apartamentos, {} residentes, {} pagos, {} reservas, "
                            + "{} visitantes y {} incidencias.",
                    apartamentos.count(), residentes.count(), pagos.count(),
                    reservas.count(), visitantes.count(), incidencias.count());
            log.info("Perfil demo: acceso administrador con {}", adminEmail);
        };
    }

    private Usuario crearUsuario(UsuarioRepository usuarios, PasswordEncoder encoder,
                                 String nombre, String email, String password, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        usuario.setPassword(encoder.encode(password));
        usuario.setRol(rol);
        usuario.setDebeCambiarPassword(false);
        return usuarios.save(usuario);
    }

    private List<Apartamento> crearApartamentos(ApartamentoRepository repo) {
        List<Apartamento> creados = new ArrayList<>();
        String[] torres = {"A", "B", "C"};
        for (String torre : torres) {
            for (int piso = 1; piso <= 4; piso++) {
                for (int puerta = 1; puerta <= 2; puerta++) {
                    String numero = torre + piso + "0" + puerta;
                    // La torre C queda parcialmente libre para mostrar el estado
                    // DISPONIBLE en los listados.
                    String estado = "C".equals(torre) && piso >= 3 ? "DISPONIBLE" : "OCUPADO";
                    if ("B".equals(torre) && piso == 4 && puerta == 2) {
                        estado = "MANTENIMIENTO";
                    }
                    creados.add(repo.save(new Apartamento(numero, torre, piso, estado)));
                }
            }
        }
        return creados;
    }

    private List<Residente> crearResidentes(ResidenteRepository repo,
                                            ResidenteApartamentoRepository asociaciones,
                                            UsuarioRepository usuarios,
                                            PasswordEncoder encoder,
                                            List<Apartamento> aptos) {
        String[][] personas = {
                {"Maria Garcia", "10234567", "3007654321", "maria"},
                {"Carlos Rodriguez", "11223344", "3001122334", "carlos"},
                {"Ana Betancur", "12345678", "3012345678", "ana"},
                {"Julian Mesa", "13456789", "3023456789", null},
                {"Sofia Naranjo", "14567890", "3034567890", null},
                {"Diego Ospina", "15678901", "3045678901", null},
                {"Valentina Cruz", "16789012", "3056789012", null},
                {"Andres Quintero", "17890123", "3067890123", null},
        };

        List<Residente> creados = new ArrayList<>();
        for (int i = 0; i < personas.length; i++) {
            String[] datos = personas[i];
            Apartamento apto = aptos.get(i);

            Residente residente = new Residente();
            residente.setNombre(datos[0]);
            residente.setDocumento(datos[1]);
            residente.setTelefono(datos[2]);
            residente.setApartamento(apto);

            String usuarioBase = datos[3];
            if (usuarioBase != null) {
                String email = usuarioBase + DOMINIO;
                residente.setCorreo(email);
                Usuario usuario = crearUsuario(usuarios, encoder, datos[0], email,
                        residentePassword, Rol.RESIDENTE);
                residente.setUsuario(usuario);
            } else {
                residente.setCorreo(usuarioNormalizado(datos[0]) + DOMINIO);
            }

            Residente guardado = repo.save(residente);
            creados.add(guardado);

            ResidenteApartamento asociacion = new ResidenteApartamento();
            asociacion.setResidente(guardado);
            asociacion.setApartamento(apto);
            asociacion.setFechaAsignacion(LocalDate.now().minusMonths(18L - i));
            asociacion.setActivo(true);
            asociaciones.save(asociacion);
        }
        return creados;
    }

    private String usuarioNormalizado(String nombre) {
        return nombre.toLowerCase().split(" ")[0];
    }

    private void crearPagos(PagoRepository repo, List<Residente> residentes) {
        LocalDate hoy = LocalDate.now();
        // Seis meses de cuotas de administracion: los meses cerrados quedan
        // pagados y el mes en curso reparte pendientes y vencidos.
        for (int mes = 5; mes >= 0; mes--) {
            LocalDate periodo = hoy.minusMonths(mes).withDayOfMonth(1);
            for (int i = 0; i < residentes.size(); i++) {
                Residente residente = residentes.get(i);
                Pago pago = new Pago();
                pago.setResidente(residente);
                pago.setApartamento(residente.getApartamento());
                pago.setMonto(new BigDecimal("320000.00"));
                pago.setTipoPago(TipoPago.ADMINISTRACION);
                pago.setFecha(periodo);
                pago.setFechaVencimiento(periodo.plusDays(9));
                pago.setReferenciaPago("ADM-%d%02d-%03d"
                        .formatted(periodo.getYear(), periodo.getMonthValue(), i + 1));

                if (mes > 0) {
                    pago.setEstadoPago(EstadoPago.PAGADO);
                    pago.setMetodo(MetodoPago.values()[i % MetodoPago.values().length]);
                } else if (i % 4 == 0) {
                    pago.setEstadoPago(EstadoPago.VENCIDO);
                    pago.setFechaVencimiento(hoy.minusDays(5));
                } else if (i % 3 == 0) {
                    pago.setEstadoPago(EstadoPago.PENDIENTE);
                } else {
                    pago.setEstadoPago(EstadoPago.PAGADO);
                    pago.setMetodo(MetodoPago.PSE);
                }
                pago.setCreadoEn(periodo.atTime(LocalTime.of(9, 0)));
                repo.save(pago);
            }
        }

        // Un par de multas para que el filtro por tipo tenga algo que mostrar.
        Residente moroso = residentes.get(1);
        Pago multa = new Pago();
        multa.setResidente(moroso);
        multa.setApartamento(moroso.getApartamento());
        multa.setMonto(new BigDecimal("150000.00"));
        multa.setTipoPago(TipoPago.MULTA);
        multa.setEstadoPago(EstadoPago.PENDIENTE);
        multa.setFecha(hoy.minusDays(12));
        multa.setFechaVencimiento(hoy.plusDays(3));
        multa.setReferenciaPago("MUL-%d-001".formatted(hoy.getYear()));
        multa.setCreadoEn(hoy.minusDays(12).atTime(LocalTime.of(16, 30)));
        repo.save(multa);
    }

    private void crearReservas(ReservaRepository repo, List<Residente> residentes) {
        LocalDateTime base = LocalDate.now().atTime(LocalTime.of(10, 0));
        TipoEspacio[] espacios = TipoEspacio.values();
        EstadoReserva[] estados = {
                EstadoReserva.APROBADA, EstadoReserva.PENDIENTE,
                EstadoReserva.APROBADA, EstadoReserva.RECHAZADA,
        };
        String[] notas = {
                "Cumpleanos infantil, 15 invitados.",
                "Reunion familiar de fin de semana.",
                "Entrenamiento personal, franja de la manana.",
                "Se cruza con el mantenimiento programado.",
        };

        for (int i = 0; i < 8; i++) {
            Residente residente = residentes.get(i % residentes.size());
            Reserva reserva = new Reserva();
            reserva.setResidente(residente);
            reserva.setApartamento(residente.getApartamento());
            reserva.setTipoEspacio(espacios[i % espacios.length]);
            // Cuatro reservas pasadas y cuatro futuras.
            LocalDateTime inicio = base.plusDays(i - 4L).plusHours(i % 3);
            reserva.setFechaInicio(inicio);
            reserva.setFechaFin(inicio.plusHours(4));
            reserva.setEstado(estados[i % estados.length]);
            reserva.setObservaciones(notas[i % notas.length]);
            reserva.setCreadoEn(inicio.minusDays(6));
            repo.save(reserva);
        }
    }

    private void crearVisitantes(VisitanteRepository repo, List<Apartamento> aptos) {
        String[][] gente = {
                {"Pedro Alvarez", "20345678"},
                {"Luisa Fernanda Rios", "21456789"},
                {"Mensajeria Envia", "22567890"},
                {"Camila Torres", "23678901"},
                {"Tecnico ascensores", "24789012"},
                {"Jorge Salazar", "25890123"},
        };

        LocalDateTime ahora = LocalDateTime.now();
        for (int i = 0; i < gente.length; i++) {
            Visitante visitante = new Visitante();
            visitante.setNombre(gente[i][0]);
            visitante.setDocumento(gente[i][1]);
            visitante.setApartamento(aptos.get(i));
            LocalDateTime entrada = ahora.minusDays(i).minusHours(3L + i);
            visitante.setFechaEntrada(entrada);
            // Los dos ultimos siguen dentro: alimentan el contador de visitantes activos.
            if (i < gente.length - 2) {
                visitante.setFechaSalida(entrada.plusHours(2));
            }
            repo.save(visitante);
        }
    }

    private void crearIncidencias(IncidenciaRepository repo, List<Residente> residentes,
                                  Usuario admin) {
        record Caso(String titulo, String descripcion, String categoria,
                    PrioridadIncidencia prioridad, EstadoIncidencia estado, String cierre) { }

        List<Caso> casos = List.of(
                new Caso("Fuga en el bajante del bano",
                        "Hay goteo constante en el bajante del bano social; moja el cielorraso del piso inferior.",
                        "Plomeria", PrioridadIncidencia.CRITICA, EstadoIncidencia.EN_PROCESO, null),
                new Caso("Luz fundida en el parqueadero",
                        "Las dos lamparas del sotano frente a las celdas 12 y 13 llevan una semana apagadas.",
                        "Electricidad", PrioridadIncidencia.MEDIA, EstadoIncidencia.PENDIENTE, null),
                new Caso("Ascensor de la torre B se detiene entre pisos",
                        "El ascensor se frena entre el tercer y el cuarto piso y hay que forzar la puerta.",
                        "Ascensores", PrioridadIncidencia.ALTA, EstadoIncidencia.EN_PROCESO, null),
                new Caso("Ruido de obra fuera de horario",
                        "Remodelacion en el 402 con taladro despues de las diez de la noche.",
                        "Convivencia", PrioridadIncidencia.BAJA, EstadoIncidencia.RESUELTA,
                        "Se notifico al propietario y se acordo horario de siete a seis."),
                new Caso("Porton vehicular no responde al control",
                        "El porton abre solo con el boton de porteria; los controles remotos no funcionan.",
                        "Seguridad", PrioridadIncidencia.ALTA, EstadoIncidencia.RESUELTA,
                        "Se reprogramaron los controles y se cambio la bateria del receptor."),
                new Caso("Solicitud de parqueadero adicional",
                        "Pido asignacion de una segunda celda de parqueo para visitantes permanentes.",
                        "Administrativo", PrioridadIncidencia.BAJA, EstadoIncidencia.RECHAZADA,
                        "No hay celdas libres; la solicitud queda en lista de espera.")
        );

        for (int i = 0; i < casos.size(); i++) {
            Caso caso = casos.get(i);
            Residente residente = residentes.get(i % residentes.size());

            Incidencia incidencia = new Incidencia();
            incidencia.setTitulo(caso.titulo());
            incidencia.setDescripcion(caso.descripcion());
            incidencia.setCategoria(caso.categoria());
            incidencia.setPrioridad(caso.prioridad());
            incidencia.setEstado(caso.estado());
            incidencia.setResidente(residente);
            incidencia.setApartamento(residente.getApartamento());

            if (caso.estado() == EstadoIncidencia.RECHAZADA) {
                incidencia.setMotivoRechazo(caso.cierre());
            } else if (caso.estado() == EstadoIncidencia.RESUELTA) {
                incidencia.setObservacionResolucion(caso.cierre());
            }

            incidencia.getHistorial().add(new IncidenciaHistorial(incidencia, residente.getUsuario(),
                    null, EstadoIncidencia.PENDIENTE, "Incidencia reportada por el residente."));
            if (caso.estado() != EstadoIncidencia.PENDIENTE) {
                incidencia.getHistorial().add(new IncidenciaHistorial(incidencia, admin,
                        EstadoIncidencia.PENDIENTE, caso.estado(),
                        caso.cierre() != null ? caso.cierre() : "Se asigno al equipo de mantenimiento."));
            }

            repo.save(incidencia);
        }
    }

    private void crearAuditoria(AuditoriaEventoRepository repo, Usuario admin, Usuario porteria) {
        repo.save(new AuditoriaEvento(admin, "CARGA_DEMO", "SISTEMA", "Instalacion", null,
                "EXITO", "Conjunto de datos de demostracion cargado al iniciar la aplicacion."));
        repo.save(new AuditoriaEvento(porteria, "REGISTRO_VISITANTE", "PORTERIA", "Visitante", null,
                "EXITO", "Registro de ingreso de visitantes del turno."));
    }
}
