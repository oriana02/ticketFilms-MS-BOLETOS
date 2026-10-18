package com.ticketfilms.ms_boletos.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EntradaGeneralDto {

    @NotNull
    private Long sectorId;

    @NotNull
    private String sector;

    private String tipoAcceso; // opcional: GENERAL | VIP

    @NotNull
    @Min(1)
    private Integer cantidad;

    @NotNull
    private BigDecimal precioUnitario;
}
