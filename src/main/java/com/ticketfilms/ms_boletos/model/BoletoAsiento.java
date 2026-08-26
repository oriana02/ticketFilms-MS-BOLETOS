package com.ticketfilms.ms_boletos.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Mapea 1:1 la tabla `boleto_asiento`. asiento_id es referencia lógica a
// ms-asientos; fila/numero/categoria/precio_pagado van denormalizados como
// snapshot de la compra, para que el historial no cambie si el mapa de
// asientos de ms-asientos se regenera después.
@Entity
@Table(
        name = "boleto_asiento",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_boletoasiento_boleto_asiento",
                columnNames = {"boleto_id", "asiento_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoletoAsiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boleto_id", nullable = false, foreignKey = @ForeignKey(name = "fk_boletoasiento_boleto"))
    private Boleto boleto;

    @Column(name = "asiento_id", nullable = false)
    private Long asientoId;

    @Column(name = "fila", nullable = false, length = 2)
    private String fila;

    @Column(name = "numero", nullable = false)
    private Integer numero;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false, length = 20)
    private CategoriaAsiento categoria;

    @Column(name = "precio_pagado", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioPagado;
}
