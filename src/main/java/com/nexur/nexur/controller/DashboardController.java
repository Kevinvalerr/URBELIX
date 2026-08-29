package com.nexur.nexur.controller;

import com.nexur.nexur.model.DashboardActivity;
import com.nexur.nexur.model.Incidencia;
import com.nexur.nexur.model.Pago;
import com.nexur.nexur.model.Reserva;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.model.enums.EstadoReserva;
import com.nexur.nexur.service.ApartamentoService;
import com.nexur.nexur.service.PagoService;
import com.nexur.nexur.service.ReservaService;
import com.nexur.nexur.service.ResidenteService;
import com.nexur.nexur.service.VisitanteService;
import com.nexur.nexur.service.IncidenciaService;
import com.nexur.nexur.service.NotificacionService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    private final ApartamentoService apartamentoService;
    private final ResidenteService residenteService;
    private final PagoService pagoService;
    private final ReservaService reservaService;
    private final VisitanteService visitanteService;
    private final IncidenciaService incidenciaService;
    private final NotificacionService notificacionService;

    public DashboardController(ApartamentoService apartamentoService,
                               ResidenteService residenteService,
                               PagoService pagoService,
                               ReservaService reservaService,
                               VisitanteService visitanteService,
                               IncidenciaService incidenciaService,
                               NotificacionService notificacionService) {
        this.apartamentoService = apartamentoService;
        this.residenteService = residenteService;
        this.pagoService = pagoService;
        this.reservaService = reservaService;
        this.visitanteService = visitanteService;
        this.incidenciaService = incidenciaService;
        this.notificacionService = notificacionService;
    }

   @GetMapping("/dashboard")
public String mostrarDashboard(Model model, Principal principal, Authentication authentication) {

        model.addAttribute("titulo", "Dashboard");
        model.addAttribute("currentPath", "/dashboard");
        model.addAttribute("notificacionesNoLeidas", notificacionService.contarNoLeidas(authentication.getName()));

        //  DEFINIR UNA SOLA VEZ
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        boolean isResidente = authentication != null && authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_RESIDENTE"));
        boolean isPorteria = authentication != null && authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_PORTERIA"));

        String email = authentication.getName();

        //  PAGOS VENCIDOS FILTRADOS
        if (isAdmin) {
            model.addAttribute("pagosVencidos", pagoService.obtenerPagosVencidos());
        } else if (isResidente) {
            model.addAttribute("pagosVencidos", pagoService.obtenerPagosVencidosPorUsuario(email));
        } else {
            model.addAttribute("pagosVencidos", List.of());
        }

        String usuarioNombre = "Usuario";
        String usuarioEmail = null;
        String rolTexto = "Usuario";
        String rolCodigo = "USUARIO";
        if (authentication != null && authentication.getPrincipal() instanceof Usuario usuario) {
            usuarioNombre = usuario.getNombre() != null ? usuario.getNombre() : usuario.getUsername();
            usuarioEmail = usuario.getUsername();
            if (usuario.getRol() != null) {
                rolCodigo = usuario.getRol().name();
                rolTexto = switch (usuario.getRol()) {
                    case ADMIN -> "Administrador";
                    case RESIDENTE -> "Residente";
                    case PORTERIA -> "Portería";
                    default -> usuario.getRol().name();
                };
            }
        } else if (principal != null) {
            usuarioNombre = principal.getName();
        }
        final String finalUsuarioActual = usuarioNombre;
        final String finalUsuarioUsername = usuarioEmail;
        model.addAttribute("currentUser", finalUsuarioActual);
        model.addAttribute("currentRole", rolCodigo);
        model.addAttribute("currentRoleName", rolTexto);

    String miApartamento;
    List<Pago> misPagos = List.of();
    List<Reserva> misReservas = List.of();

    if (isAdmin) {
        List<Pago> todosLosPagos = pagoService.listarPagos();
        var apartamentos = apartamentoService.listarApartamentos();
        var residentes = residenteService.obtenerTodos();
        var incidencias = incidenciaService.listarTodas();
        model.addAttribute("apartamentosCount", apartamentos.size());
        model.addAttribute("residentesCount", residentes.size());
        model.addAttribute("pagosCount", todosLosPagos.size());
        model.addAttribute("morasCount", todosLosPagos.stream().filter(pago -> pago.getEstadoPago() == com.nexur.nexur.model.enums.EstadoPago.VENCIDO).count());
        model.addAttribute("multasCount", todosLosPagos.stream().filter(pago -> pago.getTipoPago() == com.nexur.nexur.model.enums.TipoPago.MULTA && pago.getEstadoPago() != com.nexur.nexur.model.enums.EstadoPago.PAGADO).count());
        model.addAttribute("reservasPendientes", reservaService.contarReservasPendientes());
        model.addAttribute("reservasCount", reservaService.contarReservasPendientes());
        long apartamentosOcupados = residentes.stream()
                .map(Residente::getApartamento)
                .filter(apartamento -> apartamento != null && apartamento.getId() != null)
                .map(apartamento -> apartamento.getId())
                .distinct()
                .count();
        BigDecimal totalFacturado = todosLosPagos.stream()
                .map(Pago::getMonto)
                .filter(monto -> monto != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRecaudado = todosLosPagos.stream()
                .filter(pago -> pago.getEstadoPago() == com.nexur.nexur.model.enums.EstadoPago.PAGADO)
                .map(Pago::getMonto)
                .filter(monto -> monto != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long incidenciasAtendidas = incidencias.stream()
                .filter(incidencia -> incidencia.getEstado() == com.nexur.nexur.model.EstadoIncidencia.RESUELTA
                        || incidencia.getEstado() == com.nexur.nexur.model.EstadoIncidencia.CERRADA)
                .count();
        model.addAttribute("ocupacionPorcentaje", porcentaje(apartamentosOcupados, apartamentos.size()));
        model.addAttribute("cobranzaPorcentaje", porcentaje(totalRecaudado, totalFacturado));
        model.addAttribute("incidenciasAtendidasPorcentaje", porcentaje(incidenciasAtendidas, incidencias.size()));
        miApartamento = null;
    } else if (isResidente) {
        misPagos = pagoService.listarPagosPorUsuario(email);
        Residente residenteActual = residenteService.buscarPorUsuarioEmail(email);
        misReservas = reservaService.listarReservasPorResidente(residenteActual.getId());
        if (residenteActual.getApartamento() != null && residenteActual.getApartamento().getId() != null) {
        }

        model.addAttribute("misPagosCount", misPagos.size());
        model.addAttribute("pagosPendientes", misPagos.stream().filter(pago -> pago.getEstadoPago() == com.nexur.nexur.model.enums.EstadoPago.PENDIENTE || pago.getEstadoPago() == com.nexur.nexur.model.enums.EstadoPago.VENCIDO).count());
        model.addAttribute("pagosAlDia", misPagos.stream().filter(pago -> pago.getEstadoPago() == com.nexur.nexur.model.enums.EstadoPago.PAGADO).count());
        model.addAttribute("misReservasCount", misReservas.size());
        model.addAttribute("misPagos", misPagos);
        model.addAttribute("misReservas", misReservas);
        model.addAttribute("reservasActivas", misReservas);
        boolean tieneMora = misPagos.stream().anyMatch(pago -> pago.getEstadoPago() == com.nexur.nexur.model.enums.EstadoPago.VENCIDO);
        boolean tienePendientes = misPagos.stream().anyMatch(pago -> pago.getEstadoPago() == com.nexur.nexur.model.enums.EstadoPago.PENDIENTE);
        model.addAttribute("estadoPago", tieneMora ? "MORA" : (tienePendientes ? "PENDIENTE" : "AL_DIA"));

        String apartamentoAsignado = null;
        for (Pago pago : misPagos) {
            if (pago.getApartamento() != null) {
                String numero = pago.getApartamento().getNumero();
                if (numero != null && !numero.isBlank()) {
                    apartamentoAsignado = numero;
                    break;
                }
            }
        }
        if (apartamentoAsignado == null) {
            for (Reserva reserva : misReservas) {
                if (reserva.getApartamento() != null) {
                    String numero = reserva.getApartamento().getNumero();
                    if (numero != null && !numero.isBlank()) {
                        apartamentoAsignado = numero;
                        break;
                    }
                }
            }
        }
        miApartamento = apartamentoAsignado != null ? apartamentoAsignado : "Sin apartamento asignado";

        model.addAttribute("miApartamento", miApartamento);
        model.addAttribute("visitantesActivosCount", residenteActual.getApartamento() == null
            ? 0
            : visitanteService.listarVisitantesActivosPorApartamento(residenteActual.getApartamento().getId()).size());
        List<String> notificaciones = new ArrayList<>();
        if (!pagoService.obtenerPagosVencidosPorUsuario(email).isEmpty()) {
            notificaciones.add("Tienes pagos vencidos pendientes de revisión.");
        } else if (misPagos.stream().anyMatch(pago -> pago.getEstadoPago() == com.nexur.nexur.model.enums.EstadoPago.PENDIENTE)) {
            notificaciones.add("Tienes pagos pendientes por revisar.");
        }
        List<String> notificacionesReservas = misReservas.stream()
                .filter(reserva -> reserva.getEstado() != EstadoReserva.PENDIENTE)
                .map(reserva -> "Reserva #" + reserva.getId() + " en " + reserva.getTipoEspacio() + " ha sido " + reserva.getEstado().name().toLowerCase() + ". " +
                        (reserva.getObservaciones() != null ? reserva.getObservaciones() : ""))
                .collect(Collectors.toList());
        if (!notificacionesReservas.isEmpty()) {
            notificaciones.add("Actualizaciones de reservas:");
            notificaciones.addAll(notificacionesReservas);
        }
        model.addAttribute("notificaciones", notificaciones);
        model.addAttribute("notificacionesCount", notificaciones.size());
        model.addAttribute("ultimaNotificacion", notificaciones.isEmpty() ? null : notificaciones.get(0));
        model.addAttribute("proximaReserva", misReservas.stream()
                .filter(reserva -> reserva.getFechaInicio() != null)
                .sorted(Comparator.comparing(Reserva::getFechaInicio))
                .findFirst()
                .map(reserva -> reserva.getTipoEspacio() + " - " + reserva.getFechaInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .orElse(null));
        model.addAttribute("estadoMora", tieneMora ? "Tienes pagos vencidos" : "No hay mora registrada");
        boolean tieneMultas = misPagos.stream().anyMatch(pago -> pago.getTipoPago() != null
                && pago.getTipoPago() == com.nexur.nexur.model.enums.TipoPago.MULTA
                && pago.getEstadoPago() != com.nexur.nexur.model.enums.EstadoPago.PAGADO);
        model.addAttribute("multa", tieneMultas ? "Tienes una multa pendiente" : "No se han registrado multas en tu cuenta");
        model.addAttribute("parqueaderoHorario", "Lunes a sábado: 06:00 - 22:00");
    } else if (isPorteria) {
        model.addAttribute("visitantesActivosCount", visitanteService.listarVisitantesActivos().size());
        model.addAttribute("solicitudesPendientes", visitanteService.listarSolicitudesPendientes().size());
        miApartamento = null;
    } else {
        model.addAttribute("visitantesActivosCount", 0);
        miApartamento = null;
    }
    model.addAttribute("incidenciasAbiertasCount", isAdmin ? incidenciaService.contarAbiertas() : 0);
    final String apartamentoActual = miApartamento;

    List<DashboardActivity> actividades = new ArrayList<>();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    List<Pago> pagosActividad = isAdmin ? pagoService.listarPagos() : misPagos;
    for (Pago pago : pagosActividad) {
            actividades.add(new DashboardActivity(
                    pago.getResidente() != null ? pago.getResidente().getNombre() : "—",
                    "Registró pago de " + pago.getMonto() + " para apto " + (pago.getApartamento() != null ? pago.getApartamento().getNumero() : "—"),
                    pago.getCreadoEn() != null ? pago.getCreadoEn().format(formatter) : "Sin fecha",
                    "Pago",
                    pago.getCreadoEn())
            );
    }

            List<Reserva> reservasActividad = isAdmin ? reservaService.listarReservas() : misReservas;
        for (Reserva reserva : reservasActividad) {
                actividades.add(new DashboardActivity(
                        reserva.getResidente() != null ? reserva.getResidente().getNombre() : "N/A",
                        "Solicitó reserva en " + reserva.getTipoEspacio() + " para apto " + (reserva.getApartamento() != null ? reserva.getApartamento().getNumero() : "—"),
                        reserva.getCreadoEn() != null ? reserva.getCreadoEn().format(formatter) : "Sin fecha",
                        "Reserva",
                        reserva.getCreadoEn())
                );
        }

        if (isAdmin) {
            for (Incidencia incidencia : incidenciaService.listarTodas()) {
                actividades.add(new DashboardActivity(
                        incidencia.getResidente() != null ? incidencia.getResidente().getNombre() : "Residente",
                        "Registro de incidencia: " + incidencia.getAsunto(),
                        incidencia.getCreadoEn() != null ? incidencia.getCreadoEn().format(formatter) : "Sin fecha",
                        "Incidencia",
                        incidencia.getCreadoEn())
                );
            }
        }

        actividades.sort(Comparator.comparing(DashboardActivity::getFechaHora, Comparator.nullsLast(Comparator.reverseOrder())));

        if (actividades.isEmpty()) {
            actividades.add(new DashboardActivity("Sistema", "Aún no hay actividad registrada.", "—", "Info", null));
        }

        model.addAttribute("actividades", actividades);
        return "dashboard/dashboard";
    }

    private int porcentaje(long parte, long total) {
        if (total == 0) {
            return 0;
        }
        return (int) Math.round((parte * 100.0) / total);
    }

    private int porcentaje(BigDecimal parte, BigDecimal total) {
        if (total == null || total.signum() <= 0 || parte == null) {
            return 0;
        }
        return parte.multiply(BigDecimal.valueOf(100))
                .divide(total, 0, java.math.RoundingMode.HALF_UP)
                .intValue();
    }
}
