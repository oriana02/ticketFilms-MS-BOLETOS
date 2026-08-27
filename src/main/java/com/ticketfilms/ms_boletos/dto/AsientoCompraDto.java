package com.ticketfilms.ms_boletos.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AsientoCompraDto {

    @NotNull
    private Long asientoId;

    @NotNull
    private String fila;

    @NotNull
    private Integer numero;

    @NotNull
    private String categoria; // "ESTANDAR" | "PREMIUM"

    @NotNull
    private BigDecimal precio;
}
