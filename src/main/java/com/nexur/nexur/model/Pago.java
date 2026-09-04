package com.nexur.nexur.model;

import jakarta.persistence.*;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.nexur.nexur.model.enums.EstadoPago;
import com.nexur.nexur.model.enums.MetodoPago;
import com.nexur.nexur.model.enums.TipoPago;

@Entity
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "residente_id")
private Residente residente;

    @NotNull(message = "El monto es obligatorio")
@Column(nullable = false)
private java.math.BigDecimal monto;

   @Enumerated(EnumType.STRING)
private MetodoPago metodo;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @Column(name = "fecha_pago")
    private LocalDate fechaPago;

    @Enumerated(EnumType.STRING)
@Column(nullable = false)
private EstadoPago estadoPago;

@Enumerated(EnumType.STRING)
private TipoPago tipoPago;

private LocalDate fechaVencimiento;

    @Column(unique = true)
    private String referenciaPago;

    @Column(name = "resultado_simulacion", length = 30)
    private String resultadoSimulacion;

    @Column(name = "transaccion_simulada", length = 80)
    private String transaccionSimulada;

    @Column(name = "simulado_en")
    private LocalDateTime simuladoEn;

    private LocalDateTime creadoEn;

    @ManyToOne
    @JoinColumn(name = "apartamento_id")
    private Apartamento apartamento;

    public Pago() {
        this.creadoEn = LocalDateTime.now();
       this.estadoPago = EstadoPago.PENDIENTE;
    }

    public Long getId() {
        return id;
    }

    public EstadoPago getEstadoPago() {
    return estadoPago;
}


public void setEstadoPago(EstadoPago estadoPago) {
    this.estadoPago = estadoPago;
}
public Residente getResidente() {
    return residente;
}

public void setResidente(Residente residente) {
    this.residente = residente;
}

public java.math.BigDecimal getMonto() {
    return monto;
}

public void setMonto(java.math.BigDecimal monto) {
    this.monto = monto;
}

 public MetodoPago getMetodo() {
    return metodo;
}

public void setMetodo(MetodoPago metodo) {
    this.metodo = metodo;
}

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDate fechaPago) {
        this.fechaPago = fechaPago;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }

    public Apartamento getApartamento() {
        return apartamento;
    }

    public void setApartamento(Apartamento apartamento) {
        this.apartamento = apartamento;
    }

    public TipoPago getTipoPago() {
    return tipoPago;
}

public void setTipoPago(TipoPago tipoPago) {
    this.tipoPago = tipoPago;
}

public LocalDate getFechaVencimiento() {
    return fechaVencimiento;
}

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
}

public String getReferenciaPago() {
    return referenciaPago;
}

public void setReferenciaPago(String referenciaPago) {
    this.referenciaPago = referenciaPago;
}

public String getResultadoSimulacion() {
    return resultadoSimulacion;
}

public void setResultadoSimulacion(String resultadoSimulacion) {
    this.resultadoSimulacion = resultadoSimulacion;
}

public String getTransaccionSimulada() {
    return transaccionSimulada;
}

public void setTransaccionSimulada(String transaccionSimulada) {
    this.transaccionSimulada = transaccionSimulada;
}

public LocalDateTime getSimuladoEn() {
    return simuladoEn;
}

public void setSimuladoEn(LocalDateTime simuladoEn) {
    this.simuladoEn = simuladoEn;
}
}
