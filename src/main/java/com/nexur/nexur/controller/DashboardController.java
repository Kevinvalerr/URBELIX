package com.nexur.nexur.controller;

import com.nexur.nexur.model.DashboardActivity;
import com.nexur.nexur.model.Pago;
import com.nexur.nexur.model.Reserva;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.model.Visitante;
import com.nexur.nexur.model.enums.EstadoReserva;
import com.nexur.nexur.service.ApartamentoService;
import com.nexur.nexur.service.PagoService;
import com.nexur.nexur.service.ReservaService;
import com.nexur.nexur.service.ResidenteService;
import com.nexur.nexur.service.VisitanteService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.time.format.DateTimeFormatter;
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

    public DashboardController(ApartamentoService apartamentoService,
                               ResidenteService residenteService,
                               PagoService pagoService,
                               ReservaService reservaService,
                               VisitanteService visitanteService) {
        this.apartamentoService = apartamentoService;
        this.residenteService = residenteService;
        this.pagoService = pagoService;
        this.reservaService = reservaService;
        this.visitanteService = visitanteService;
    }

   @GetMapping("/dashboard")
public String mostrarDashboard(Model model, Principal principal, Authentication authentication) {

        model.addAttribute("titulo", "Dashboard");
        model.addAttribute("currentPath", "/dashboard");

        //  DEFINIR UNA SOLA VEZ
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        String email = authentication.getName();

        //  PAGOS VENCIDOS FILTRADOS
        if (isAdmin) {
            model.addAttribute("pagosVencidos", pagoService.obtenerPagosVencidos());
        } else {
            model.addAttribute("pagosVencidos", pagoService.obtenerPagosVencidosPorUsuario(email));
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

    if (isAdmin) {
        model.addAttribute("apartamentosCount", apartamentoService.listarApartamentos().size());
        model.addAttribute("residentesCount", residenteService.obtenerTodos().size());
        model.addAttribute("pagosCount", pagoService.contarPagos());
        model.addAttribute("reservasCount", reservaService.contarReservasPendientes());
        model.addAttribute("visitantesActivosCount", visitanteService.listarVisitantesActivos().size());
        miApartamento = null;
    } else {
        List<Pago> misPagos = pagoService.listarPagos().stream()
                .filter(pago -> pago.getResidente() != null && (
                        pago.getResidente().getNombre().equalsIgnoreCase(finalUsuarioActual) ||
                        (finalUsuarioUsername != null && pago.getResidente().getNombre().equalsIgnoreCase(finalUsuarioUsername))
                ))
                .collect(Collectors.toList());
        List<Reserva> misReservas = reservaService.listarReservas().stream()
                .filter(reserva -> reserva.getResidente() != null && (
                        reserva.getResidente().getNombre().equalsIgnoreCase(finalUsuarioActual) ||
                        (finalUsuarioUsername != null && reserva.getResidente().getNombre().equalsIgnoreCase(finalUsuarioUsername))
                ))
                .collect(Collectors.toList());

        model.addAttribute("misPagosCount", misPagos.size());
        model.addAttribute("misReservasCount", misReservas.size());
        model.addAttribute("misPagos", misPagos);
        model.addAttribute("misReservas", misReservas);
        model.addAttribute("reservasActivas", misReservas);
        model.addAttribute("estadoPago", misPagos.isEmpty() ? "AL_DIA" : "PENDIENTE");

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
        model.addAttribute("visitantesActivosCount", visitanteService.listarVisitantesActivos().size());
        List<String> notificaciones = new ArrayList<>(List.of(
                "Recuerda pagar el servicio antes del día 10 para evitar moras.",
                "El parqueadero C está disponible para tu bloque.",
                "Se ha actualizado el reglamento de visitas."
        ));
        List<String> notificacionesReservas = reservaService.listarReservas().stream()
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
        model.addAttribute("estadoMora", misPagos.isEmpty() ? "No hay mora registrada" : "Revisa tus pagos pendientes con administración");
        model.addAttribute("multa", "No se han registrado multas en tu cuenta");
        model.addAttribute("parqueaderoHorario", "Lunes a sábado: 06:00 - 22:00");
    }
    final String apartamentoActual = miApartamento;

    List<DashboardActivity> actividades = new ArrayList<>();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    for (Pago pago : pagoService.listarPagos()) {
        if (isAdmin || (pago.getResidente() != null && pago.getResidente().getNombre().equalsIgnoreCase(finalUsuarioActual))) {
            actividades.add(new DashboardActivity(
                    pago.getResidente() != null ? pago.getResidente().getNombre() : "—",
                    "Registró pago de " + pago.getMonto() + " para apto " + (pago.getApartamento() != null ? pago.getApartamento().getNumero() : "—"),
                    pago.getCreadoEn() != null ? pago.getCreadoEn().format(formatter) : "Sin fecha",
                    "Pago",
                    pago.getCreadoEn())
            );
        }
    }
                        pago.getResidente() != null ? pago.getResidente().getNombre() : "—",
                        "Registró pago de " + pago.getMonto() + " para apto " + (pago.getApartamento() != null ? pago.getApartamento().getNumero() : "—"),
                        pago.getCreadoEn() != null ? pago.getCreadoEn().format(formatter) : "Sin fecha",
                        "Pago",
                        pago.getCreadoEn())
                );
            }
        }

        for (Reserva reserva : reservaService.listarReservas()) {
            if (true) {
                actividades.add(new DashboardActivity(
                        reserva.getResidente() != null ? reserva.getResidente().getNombre() : "N/A",
                        "Solicitó reserva en " + reserva.getTipoEspacio() + " para apto " + (reserva.getApartamento() != null ? reserva.getApartamento().getNumero() : "—"),
                        reserva.getCreadoEn() != null ? reserva.getCreadoEn().format(formatter) : "Sin fecha",
                        "Reserva",
                        reserva.getCreadoEn())
                );
            }
        }

        for (Visitante visitante : visitanteService.listarVisitantes()) {
            if (isAdmin || (visitante.getApartamento() != null && visitante.getApartamento().getNumero() != null && visitante.getApartamento().getNumero().equals(apartamentoActual))) {
                actividades.add(new DashboardActivity(
                        visitante.getNombre(),
                        "Registró visita para apto " + (visitante.getApartamento() != null ? visitante.getApartamento().getNumero() : "—"),
                        visitante.getFechaEntrada() != null ? visitante.getFechaEntrada().format(formatter) : "Sin fecha",
                        "Visita",
                        visitante.getFechaEntrada())
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
}
