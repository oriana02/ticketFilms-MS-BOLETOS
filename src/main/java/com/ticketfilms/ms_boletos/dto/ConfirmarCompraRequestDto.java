package com.ticketfilms.ms_boletos.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
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
    private LocalDateTime fechaHoraFuncion;

    // opcionales (copia de lo que muestra cartelera)
    private String tipoEvento;
    private String sede;
    private String ciudad;
    private String puerta;

    // sectores numerados; puede venir vacío si solo compra entradas generales
    @Valid
    private List<AsientoCompraDto> asientos = new ArrayList<>();

    // sectores de admisión general; puede venir vacío
    @Valid
    private List<EntradaGeneralDto> entradasGenerales = new ArrayList<>();
}
