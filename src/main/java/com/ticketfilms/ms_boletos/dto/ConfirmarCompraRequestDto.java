package com.ticketfilms.ms_boletos.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmarCompraRequestDto {

    @NotNull
    private Long funcionId;

    @NotNull
    private Long eventoId;

    @NotNull
    private String tituloEvento;

    @NotNull
    private java.time.LocalDateTime fechaHoraFuncion;

    @NotEmpty
    private List<AsientoCompraDto> asientos;
}
