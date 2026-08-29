package com.nexur.nexur;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.EstadoIncidencia;
import com.nexur.nexur.model.EstadoVisitante;
import com.nexur.nexur.model.Incidencia;
import com.nexur.nexur.model.MovimientoParqueadero;
import com.nexur.nexur.model.Pago;
import com.nexur.nexur.model.Parqueadero;
import com.nexur.nexur.model.Reserva;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.model.Vehiculo;
import com.nexur.nexur.model.Visitante;
import com.nexur.nexur.model.EstadoMovimientoParqueadero;
import com.nexur.nexur.model.Rol;
import com.nexur.nexur.model.enums.EstadoPago;
import com.nexur.nexur.model.enums.TipoEspacio;
import com.nexur.nexur.model.enums.TipoPago;
import com.nexur.nexur.model.enums.MetodoPago;
import com.nexur.nexur.repository.ApartamentoRepository;
import com.nexur.nexur.repository.IncidenciaRepository;
import com.nexur.nexur.repository.MovimientoParqueaderoRepository;
import com.nexur.nexur.repository.PagoRepository;
import com.nexur.nexur.repository.ParqueaderoRepository;
import com.nexur.nexur.repository.ReservaRepository;
import com.nexur.nexur.repository.ResidenteRepository;
import com.nexur.nexur.repository.UsuarioRepository;
import com.nexur.nexur.repository.VehiculoRepository;
import com.nexur.nexur.repository.VisitanteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PDF;
import org.springframework.http.HttpHeaders;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:urbelix-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=always",
        "spring.jpa.defer-datasource-initialization=true",
        "app.seed-data.enabled=false",
        "app.payments.simulation-enabled=true",
        "app.payments.simulation-secret=integration-sandbox-secret"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NexurIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ApartamentoRepository apartamentoRepository;
    @Autowired
    private PagoRepository pagoRepository;
    @Autowired
    private IncidenciaRepository incidenciaRepository;
    @Autowired
    private ParqueaderoRepository parqueaderoRepository;
    @Autowired
    private MovimientoParqueaderoRepository movimientoParqueaderoRepository;
    @Autowired
    private ReservaRepository reservaRepository;
    @Autowired
    private ResidenteRepository residenteRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private VehiculoRepository vehiculoRepository;
    @Autowired
    private VisitanteRepository visitanteRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void prepararApartamento() {
        if (apartamentoRepository.findByNumero("901").isEmpty()) {
            Apartamento apartamento = new Apartamento();
            apartamento.setNumero("901");
            apartamento.setTorre("A");
            apartamento.setPiso(9);
            apartamento.setEstado("DISPONIBLE");
            apartamento.setCodigoRegistro("URB-901-TEST");
            apartamentoRepository.save(apartamento);
        }
    }

    @Test
    void registroPublicoRequiereCodigoResidencialValido() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("nombre", "Residente Integracion")
                        .param("email", "integracion@example.com")
                        .param("password", "Segura123!")
                        .param("confirmPassword", "Segura123!")
                        .param("documento", "90123456")
                        .param("telefono", "3001234567")
                        .param("numeroApartamento", "901")
                        .param("codigoRegistro", "URB-901-TEST"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered=true"));
    }

    @Test
    void formulariosPublicosIncluyenTokenCsrf() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
    }

    @Test
    void registroConErrorConservaLosDatosNoSensibles() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("nombre", "Residente Integracion Error")
                        .param("email", "error@example.com")
                        .param("password", "Segura123!")
                        .param("confirmPassword", "Segura123!")
                        .param("documento", "90123458")
                        .param("telefono", "3001234569")
                        .param("numeroApartamento", "901")
                        .param("codigoRegistro", "CODIGO-INCORRECTO"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"90123458\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"3001234569\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"CODIGO-INCORRECTO\"")));
    }

    @Test
    void registroRechazaCorreoDuplicadoAunqueCambieMayusculasYEspacios() throws Exception {
        Usuario existente = new Usuario();
        existente.setNombre("Cuenta Existente");
        existente.setEmail("duplicado@example.com");
        existente.setPassword(passwordEncoder.encode("Segura123!"));
        existente.setRol(Rol.RESIDENTE);
        usuarioRepository.save(existente);

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("nombre", "Nuevo Intento")
                        .param("email", "  DUPLICADO@EXAMPLE.COM  ")
                        .param("password", "Segura123!")
                        .param("confirmPassword", "Segura123!")
                        .param("documento", "90123462")
                        .param("telefono", "3001234573")
                        .param("numeroApartamento", "901")
                        .param("codigoRegistro", "URB-901-TEST"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("El email ya está en uso")));
    }

    @Test
    void loginRealAceptaCorreoConMayusculasYEspacios() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setNombre("Administrador Login");
        usuario.setEmail("login.real@example.com");
        usuario.setPassword(passwordEncoder.encode("Segura123!"));
        usuario.setRol(Rol.ADMIN);
        usuario.setActivo(true);
        usuario.setDebeCambiarPassword(false);
        usuarioRepository.save(usuario);

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "  LOGIN.REAL@EXAMPLE.COM  ")
                        .param("password", "Segura123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void residenteNoPuedeAbrirReportes() throws Exception {
        mockMvc.perform(get("/reportes").with(user("residente@example.com").roles("RESIDENTE")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/acceso-denegado"));
        mockMvc.perform(get("/parqueaderos/nuevo").with(user("residente@example.com").roles("RESIDENTE")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/acceso-denegado"));
        mockMvc.perform(get("/incidencias/1/adjuntos/1")
                        .with(user("porteria@example.com").roles("PORTERIA")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/acceso-denegado"));
    }

    @Test
    void residentePuedeAbrirSuModuloDeVehiculos() throws Exception {
        mockMvc.perform(get("/parqueaderos/mis-vehiculos")
                        .with(user("residente@example.com").roles("RESIDENTE")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mis vehículos")));
    }

    @Test
    void administradorNoPuedeUsarElModuloExclusivoDelResidente() throws Exception {
        mockMvc.perform(get("/parqueaderos/mis-vehiculos")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/acceso-denegado"));
    }

    @Test
    void administradorNoPuedeEntrarAOperacionesDePorteria() throws Exception {
        mockMvc.perform(get("/porteria").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/acceso-denegado"));
        mockMvc.perform(get("/porteria/parqueaderos").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/acceso-denegado"));
        mockMvc.perform(get("/visitantes").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/acceso-denegado"));
    }

    @Test
    void porteriaNoPuedeEntrarAAdministracionNiCrearSolicitudes() throws Exception {
        mockMvc.perform(get("/usuarios").with(user("porteria@example.com").roles("PORTERIA")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/acceso-denegado"));
        mockMvc.perform(get("/residentes").with(user("porteria@example.com").roles("PORTERIA")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/acceso-denegado"));
        mockMvc.perform(get("/reservas").with(user("porteria@example.com").roles("PORTERIA")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/acceso-denegado"));
        mockMvc.perform(get("/visitantes/nuevo").with(user("porteria@example.com").roles("PORTERIA")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/acceso-denegado"));
    }

    @Test
    void importacionDeResidentesEsExclusivaDeAdministracion() throws Exception {
        mockMvc.perform(get("/residentes/importar")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Importar residentes desde Excel")));

        mockMvc.perform(get("/residentes/importar")
                        .with(user("porteria@example.com").roles("PORTERIA")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/acceso-denegado"));

        mockMvc.perform(get("/residentes/importar")
                        .with(user("residente@example.com").roles("RESIDENTE")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/acceso-denegado"));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void administradorPuedeImportarResidenteDesdeExcel() throws Exception {
        prepararApartamentoExistente();
        byte[] archivo;
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Residentes");
            String[] columnas = {"Nombre", "Documento", "Telefono", "Correo", "Apartamento", "Torre", "Piso", "CodigoRegistro"};
            var encabezado = sheet.createRow(0);
            for (int index = 0; index < columnas.length; index++) {
                encabezado.createCell(index).setCellValue(columnas[index]);
            }
            var fila = sheet.createRow(1);
            String[] valores = {"Residente Excel", "90123470", "3001234581", "excel.integracion@example.com", "901", "A", "9", "URB-901-TEST"};
            for (int index = 0; index < valores.length; index++) {
                fila.createCell(index).setCellValue(valores[index]);
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            archivo = output.toByteArray();
        }

        MockMultipartFile multipartFile = new MockMultipartFile(
                "archivo", "residentes.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", archivo);

        mockMvc.perform(multipart("/residentes/importar")
                        .file(multipartFile)
                        .with(csrf())
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("CREADO")));

        Usuario usuario = usuarioRepository.findByEmail("excel.integracion@example.com").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(Rol.RESIDENTE, usuario.getRol());
        org.junit.jupiter.api.Assertions.assertTrue(residenteRepository.findAll().stream()
                .anyMatch(residente -> "90123470".equals(residente.getDocumento())));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void residenteSolicitaYPorteriaGestionaElCicloDeAcceso() throws Exception {
        Apartamento apartamento = prepararApartamentoExistente();
        Usuario usuario = new Usuario();
        usuario.setNombre("Residente Visitantes");
        usuario.setEmail("solicitud.visitante@example.com");
        usuario.setPassword(passwordEncoder.encode("Segura123!"));
        usuario.setRol(Rol.RESIDENTE);
        usuario.setActivo(true);
        usuario.setDebeCambiarPassword(false);

        Residente residente = new Residente("Residente Visitantes", "90123464", "3001234575", apartamento);
        residente.setUsuario(usuario);
        usuario.setResidente(residente);
        usuarioRepository.save(usuario);

        mockMvc.perform(post("/visitantes/guardar")
                        .with(csrf())
                        .with(user("solicitud.visitante@example.com").roles("RESIDENTE"))
                        .param("nombre", "Visitante Solicitado")
                        .param("documento", "90123465"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/visitantes"));

        Visitante visitante = visitanteRepository.findAll().stream()
                .filter(item -> "90123465".equals(item.getDocumento()))
                .findFirst().orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(EstadoVisitante.PENDIENTE, visitante.getEstado());
        org.junit.jupiter.api.Assertions.assertNull(visitante.getFechaEntrada());

        mockMvc.perform(post("/visitantes/{id}/aprobar", visitante.getId())
                        .with(csrf())
                        .with(user("porteria@example.com").roles("PORTERIA")))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/visitantes/{id}/entrada", visitante.getId())
                        .with(csrf())
                        .with(user("porteria@example.com").roles("PORTERIA")))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/visitantes/salida/{id}", visitante.getId())
                        .with(csrf())
                        .with(user("porteria@example.com").roles("PORTERIA")))
                .andExpect(status().is3xxRedirection());

        Visitante finalizado = visitanteRepository.findById(visitante.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(EstadoVisitante.FINALIZADA, finalizado.getEstado());
        org.junit.jupiter.api.Assertions.assertNotNull(finalizado.getFechaEntrada());
        org.junit.jupiter.api.Assertions.assertNotNull(finalizado.getFechaSalida());
    }

    @Test
    void administradorPuedeAbrirReportes() throws Exception {
        mockMvc.perform(get("/reportes").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void administradorPuedeCrearPersonalYElServidorValidaConfirmacion() throws Exception {
        mockMvc.perform(post("/usuarios/guardar")
                        .with(csrf())
                        .with(user("admin@example.com").roles("ADMIN"))
                        .param("nombre", "Portero Integracion")
                        .param("email", "portero.integracion@example.com")
                        .param("password", "Segura123!")
                        .param("confirmPassword", "Segura123!")
                        .param("rol", "PORTERIA"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios"));

        Usuario personal = usuarioRepository.findByEmail("portero.integracion@example.com").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(Rol.PORTERIA, personal.getRol());

        mockMvc.perform(post("/usuarios/guardar")
                        .with(csrf())
                        .with(user("admin@example.com").roles("ADMIN"))
                        .param("nombre", "Portero Invalido")
                        .param("email", "portero.invalido@example.com")
                        .param("password", "Segura123!")
                        .param("confirmPassword", "Otra123!")
                        .param("rol", "PORTERIA"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Las contraseñas no coinciden")));
        org.junit.jupiter.api.Assertions.assertTrue(
                usuarioRepository.findByEmail("portero.invalido@example.com").isEmpty());
    }

    @Test
    void administradorPuedeCrearResidenteConCuentaDesdeSuFormulario() throws Exception {
        Apartamento apartamento = prepararApartamentoExistente();

        mockMvc.perform(post("/residentes/guardar")
                        .with(csrf())
                        .with(user("admin@example.com").roles("ADMIN"))
                        .param("nombre", "Residente Alta Completa")
                        .param("documento", "90123463")
                        .param("telefono", "3001234574")
                        .param("apartamentoId", apartamento.getId().toString())
                        .param("usuario.email", "alta.residente@example.com")
                        .param("usuario.password", "Segura123!")
                        .param("confirmPassword", "Segura123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/residentes"));

        Usuario usuario = usuarioRepository.findByEmail("alta.residente@example.com").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(Rol.RESIDENTE, usuario.getRol());
        org.junit.jupiter.api.Assertions.assertEquals("Residente Alta Completa", usuario.getNombre());
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("Segura123!", usuario.getPassword()));
        org.junit.jupiter.api.Assertions.assertTrue(residenteRepository.findAll().stream()
                .anyMatch(residente -> "90123463".equals(residente.getDocumento())));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void administradorPuedeRenderizarDashboardConIncidencias() throws Exception {
        Apartamento apartamento = prepararApartamentoExistente();
        Residente residente = residenteRepository.save(
                new Residente("Residente Dashboard", "90123459", "3001234570", apartamento));
        Incidencia incidencia = new Incidencia();
        incidencia.setAsunto("Fuga en zona comun");
        incidencia.setDescripcion("Se requiere revisar la tuberia del pasillo.");
        incidencia.setTipo("MANTENIMIENTO");
        incidencia.setEstado(EstadoIncidencia.ABIERTA);
        incidencia.setResidente(residente);
        incidencia.setApartamento(apartamento);
        incidenciaRepository.save(incidencia);

        mockMvc.perform(get("/dashboard").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Fuga en zona comun")));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void administradorPuedeRenderizarIncidenciasConComentariosYAdjuntosVacios() throws Exception {
        Apartamento apartamento = prepararApartamentoExistente();
        Residente residente = residenteRepository.save(
                new Residente("Residente Incidencias", "90123460", "3001234571", apartamento));
        Incidencia incidencia = new Incidencia();
        incidencia.setAsunto("Luz del pasillo apagada");
        incidencia.setDescripcion("La luminaria del pasillo no enciende.");
        incidencia.setTipo("MANTENIMIENTO");
        incidencia.setEstado(EstadoIncidencia.ABIERTA);
        incidencia.setResidente(residente);
        incidencia.setApartamento(apartamento);
        incidenciaRepository.save(incidencia);

        mockMvc.perform(get("/incidencias").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Luz del pasillo apagada")));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void administradorPuedeRenderizarParqueaderosConApartamento() throws Exception {
        Parqueadero parqueadero = new Parqueadero();
        parqueadero.setNumero("P-90");
        parqueadero.setZona("Sotano de prueba");
        parqueadero.setApartamento(prepararApartamentoExistente());
        parqueaderoRepository.save(parqueadero);

        mockMvc.perform(get("/parqueaderos").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("P-90")));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void edicionDeParqueaderoConservaElApartamentoSeleccionado() throws Exception {
        Apartamento apartamento = prepararApartamentoExistente();
        Parqueadero parqueadero = new Parqueadero();
        parqueadero.setNumero("P-92");
        parqueadero.setZona("Sotano de prueba");
        parqueadero.setTipo(com.nexur.nexur.model.TipoVehiculo.CARRO);
        parqueadero.setEstado(com.nexur.nexur.model.EstadoParqueadero.ASIGNADO);
        parqueadero.setApartamento(apartamento);
        parqueadero = parqueaderoRepository.save(parqueadero);

        mockMvc.perform(get("/parqueaderos/editar/{id}", parqueadero.getId())
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Editar parqueadero")))
                .andExpect(content().string(org.hamcrest.Matchers.matchesPattern(
                        "(?s).*<option value=\"" + apartamento.getId()
                                + "\"\\s+selected=\"selected\">.*")));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void porteriaPuedeRenderizarDashboardEHistorialConMovimientos() throws Exception {
        Apartamento apartamento = prepararApartamentoExistente();
        Residente residente = residenteRepository.save(
                new Residente("Residente Porteria", "90123461", "3001234572", apartamento));
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setPlaca("TST901");
        vehiculo.setTipo(com.nexur.nexur.model.TipoVehiculo.CARRO);
        vehiculo.setResidente(residente);
        vehiculo = vehiculoRepository.save(vehiculo);

        Parqueadero parqueadero = new Parqueadero();
        parqueadero.setNumero("P-91");
        parqueadero.setZona("Sotano de prueba");
        parqueadero.setTipo(com.nexur.nexur.model.TipoVehiculo.CARRO);
        parqueadero.setEstado(com.nexur.nexur.model.EstadoParqueadero.OCUPADO);
        parqueadero.setApartamento(apartamento);
        parqueadero.setVehiculo(vehiculo);
        parqueadero = parqueaderoRepository.save(parqueadero);

        MovimientoParqueadero movimiento = new MovimientoParqueadero();
        movimiento.setVehiculo(vehiculo);
        movimiento.setParqueadero(parqueadero);
        movimiento.setFechaHoraIngreso(LocalDateTime.now());
        movimiento.setEstado(EstadoMovimientoParqueadero.DENTRO);
        movimientoParqueaderoRepository.save(movimiento);

        mockMvc.perform(get("/porteria/parqueaderos")
                        .with(user("porteria@example.com").roles("PORTERIA")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("TST901")));
        mockMvc.perform(get("/porteria/parqueaderos/historial")
                        .with(user("porteria@example.com").roles("PORTERIA")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("P-91")));
    }

    @Test
    void auditoriaRegistraMutacionesYSoloEsVisibleParaAdmin() throws Exception {
        mockMvc.perform(post("/avisos")
                        .with(csrf())
                        .with(user("admin@example.com").roles("ADMIN"))
                        .param("titulo", "Aviso auditado")
                        .param("contenido", "Registro de prueba de auditoria"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/auditoria").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/avisos")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("admin@example.com")));

        mockMvc.perform(get("/auditoria").with(user("residente@example.com").roles("RESIDENTE")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/acceso-denegado"));
    }

    @Test
    void administradorPublicaAvisosYLaComunidadLosConsulta() throws Exception {
        mockMvc.perform(post("/avisos")
                        .with(csrf())
                        .with(user("admin@example.com").roles("ADMIN"))
                        .param("titulo", "Mantenimiento programado")
                        .param("contenido", "El servicio de agua estará suspendido el viernes."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/avisos"));

        mockMvc.perform(get("/avisos").with(user("residente@example.com").roles("RESIDENTE")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mantenimiento programado")));
        mockMvc.perform(get("/avisos").with(user("porteria@example.com").roles("PORTERIA")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mantenimiento programado")));
    }

    @Test
    void usuarioPuedeAbrirSuBandejaDeNotificaciones() throws Exception {
        mockMvc.perform(get("/notificaciones")
                        .with(user("residente@example.com").roles("RESIDENTE")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Notificaciones")));
    }

    @Test
    void webhookPseRechazaFirmaAusenteSinDependerDeCsrf() throws Exception {
        mockMvc.perform(post("/webhooks/pagos")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void webhookWompiRechazaFirmaAusenteSinDependerDeCsrf() throws Exception {
        mockMvc.perform(post("/webhooks/wompi")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void porteriaPuedeAbrirSuDashboard() throws Exception {
        mockMvc.perform(get("/dashboard").with(user("porteria@example.com").roles("PORTERIA")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Alertas del Sistema"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Pagos vencidos"))));
    }

    @Test
    void primerIngresoSoloPuedeContinuarEnPerfilHastaCambiarPassword() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setNombre("Usuario primer ingreso");
        usuario.setEmail("primer-ingreso@example.com");
        usuario.setPassword(passwordEncoder.encode("Temporal123!"));
        usuario.setRol(Rol.RESIDENTE);
        usuario.setActivo(true);
        usuario.setDebeCambiarPassword(true);
        usuarioRepository.save(usuario);

        mockMvc.perform(get("/avisos")
                        .with(user("primer-ingreso@example.com").roles("RESIDENTE")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/perfil?cambiarPassword=true"));

        mockMvc.perform(get("/perfil")
                        .with(user("primer-ingreso@example.com").roles("RESIDENTE")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Cambiar contraseña")));
    }

    @Test
    void porteriaNoPuedeConsultarPagosNiReservas() throws Exception {
        mockMvc.perform(get("/pagos").with(user("porteria@example.com").roles("PORTERIA")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/acceso-denegado"));
        mockMvc.perform(get("/reservas").with(user("porteria@example.com").roles("PORTERIA")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/acceso-denegado"));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void residenteSoloPuedeVerElDetalleDeSusPagos() throws Exception {
        Apartamento apartamento = prepararApartamentoExistente();
        Usuario usuarioPropio = new Usuario();
        usuarioPropio.setNombre("Residente Propio");
        usuarioPropio.setEmail("pago.propietario@example.com");
        usuarioPropio.setPassword(passwordEncoder.encode("Segura123!"));
        usuarioPropio.setRol(Rol.RESIDENTE);
        usuarioPropio.setActivo(true);
        usuarioPropio.setDebeCambiarPassword(false);
        Residente residentePropio = new Residente("Residente Propio", "90123480", "3001234590", apartamento);
        residentePropio.setUsuario(usuarioPropio);
        usuarioPropio.setResidente(residentePropio);
        usuarioRepository.save(usuarioPropio);

        Residente residenteAjeno = new Residente("Residente Ajeno", "90123481", "3001234591", apartamento);
        residenteAjeno = residenteRepository.save(residenteAjeno);

        Pago pagoPropio = pagoDePrueba(residentePropio, "PAGO-PROPIO");
        Pago pagoAjeno = pagoDePrueba(residenteAjeno, "PAGO-AJENO");
        pagoPropio = pagoRepository.save(pagoPropio);
        pagoAjeno = pagoRepository.save(pagoAjeno);

        mockMvc.perform(get("/pagos/{id}", pagoPropio.getId())
                        .with(user("pago.propietario@example.com").roles("RESIDENTE")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("PAGO-PROPIO")));

        mockMvc.perform(get("/pagos/{id}", pagoAjeno.getId())
                        .with(user("pago.propietario@example.com").roles("RESIDENTE")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/acceso-denegado"));
    }

    @Test
    void accionesDePagoRequierenCsrf() throws Exception {
        mockMvc.perform(post("/pagos/1/confirmar")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void pantallaDeAccesoDenegadoPuedeRenderizarse() throws Exception {
        mockMvc.perform(get("/acceso-denegado").with(user("residente@example.com").roles("RESIDENTE")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Acceso restringido")));
    }

    @Test
    void enlaceDeRecuperacionSinTokenMuestraPantallaAmigable() throws Exception {
        mockMvc.perform(get("/reset-password"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Este enlace no es válido o ya expiró")));
    }

    @Test
    void administradorPuedeRenderizarPagosSinRegistros() throws Exception {
        mockMvc.perform(get("/pagos").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No hay pagos registrados")));
    }

    @Test
    void administradorPuedeRenderizarReservasSinRegistros() throws Exception {
        mockMvc.perform(get("/reservas").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No hay reservas registradas")));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void administradorPuedeRenderizarPagosYReservasConRegistros() throws Exception {
        Residente residente = new Residente("Residente Vista", "90123457", "3001234568", prepararApartamentoExistente());
        residente = residenteRepository.save(residente);

        Pago pago = new Pago();
        pago.setResidente(residente);
        pago.setApartamento(residente.getApartamento());
        pago.setMonto(new BigDecimal("250000"));
        pago.setFecha(LocalDate.now());
        pago.setFechaVencimiento(LocalDate.now().plusDays(30));
        pago.setTipoPago(TipoPago.ADMINISTRACION);
        pago.setMetodo(MetodoPago.TRANSFERENCIA);
        pago.setEstadoPago(EstadoPago.PENDIENTE);
        pagoRepository.save(pago);

        Reserva reserva = new Reserva();
        reserva.setResidente(residente);
        reserva.setApartamento(residente.getApartamento());
        reserva.setTipoEspacio(TipoEspacio.SALON_SOCIAL);
        reserva.setFechaInicio(LocalDateTime.now().plusDays(2));
        reserva.setFechaFin(LocalDateTime.now().plusDays(2).plusHours(2));
        reserva.setEstado(com.nexur.nexur.model.enums.EstadoReserva.PENDIENTE);
        reserva.setObservaciones("Prueba de renderizado");
        reservaRepository.save(reserva);

        mockMvc.perform(get("/pagos").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Administración")));
        mockMvc.perform(get("/reservas").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Salón social")));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void checkoutSandboxSoloEsVisibleParaResidenteConPagoPsePendiente() throws Exception {
        Apartamento apartamento = prepararApartamentoExistente();
        Usuario usuario = new Usuario();
        usuario.setNombre("Residente Sandbox");
        usuario.setEmail("sandbox.residente@example.com");
        usuario.setPassword(passwordEncoder.encode("Segura123!"));
        usuario.setRol(Rol.RESIDENTE);
        usuario.setActivo(true);
        usuario.setDebeCambiarPassword(false);

        Residente residente = new Residente("Residente Sandbox", "90123471", "3001234582", apartamento);
        residente.setUsuario(usuario);
        usuario.setResidente(residente);
        usuarioRepository.save(usuario);

        Pago pago = new Pago();
        pago.setResidente(residente);
        pago.setApartamento(apartamento);
        pago.setMonto(new BigDecimal("125000"));
        pago.setFecha(LocalDate.now());
        pago.setFechaVencimiento(LocalDate.now().plusDays(30));
        pago.setTipoPago(TipoPago.ADMINISTRACION);
        pago.setMetodo(MetodoPago.PSE);
        pago.setEstadoPago(EstadoPago.PENDIENTE);
        pago.setReferenciaPago("PSE-SANDBOX-INTEGRATION");
        pago = pagoRepository.save(pago);

        mockMvc.perform(get("/pagos/{id}/simulador", pago.getId())
                        .with(user("sandbox.residente@example.com").roles("RESIDENTE")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Checkout de prueba")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("PSE-SANDBOX-INTEGRATION")));

        mockMvc.perform(get("/pagos/{id}/simulador", pago.getId())
                        .with(user("porteria@example.com").roles("PORTERIA")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/acceso-denegado"));

        mockMvc.perform(get("/pagos/{id}/factura", pago.getId())
                        .with(user("sandbox.residente@example.com").roles("RESIDENTE")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("factura-pago-")));

        mockMvc.perform(get("/pagos/{id}/factura", pago.getId())
                        .with(user("porteria@example.com").roles("PORTERIA")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/acceso-denegado"));
    }

    private Apartamento prepararApartamentoExistente() {
        return apartamentoRepository.findByNumero("901").orElseThrow();
    }

    private Pago pagoDePrueba(Residente residente, String referencia) {
        Pago pago = new Pago();
        pago.setResidente(residente);
        pago.setApartamento(residente.getApartamento());
        pago.setMonto(new BigDecimal("125000"));
        pago.setFecha(LocalDate.now());
        pago.setFechaVencimiento(LocalDate.now().plusDays(30));
        pago.setTipoPago(TipoPago.ADMINISTRACION);
        pago.setMetodo(MetodoPago.TRANSFERENCIA);
        pago.setEstadoPago(EstadoPago.PENDIENTE);
        pago.setReferenciaPago(referencia);
        return pago;
    }

    @Test
    void reportesMuestraErrorParaFechaInvalida() throws Exception {
        mockMvc.perform(get("/reportes?fechaInicio=fecha-invalida")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("AAAA-MM-DD")));
    }

    @Test
    void pdfRechazaRangoDeFechasInvertido() throws Exception {
        mockMvc.perform(post("/reportes/generar-pdf")
                        .with(csrf())
                        .with(user("admin@example.com").roles("ADMIN"))
                        .param("fechaInicio", "2026-08-20")
                        .param("fechaFin", "2026-08-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void erroresDeAprobacionDeReservaNoTerminanEnError500() throws Exception {
        mockMvc.perform(post("/reservas/aprobar/999999")
                        .with(csrf())
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reservas"));

        mockMvc.perform(post("/reservas/rechazar/999999")
                        .with(csrf())
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reservas"));
    }
}
