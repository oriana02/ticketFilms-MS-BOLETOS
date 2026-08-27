package com.ticketfilms.ms_boletos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Lo que el frontend necesita para la pantalla de confirmación
// y para el historial 
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoletoResponseDto {

    private String codigoBoleto;
    private String tituloEvento;
    private LocalDateTime fechaHoraFuncion;
    private BigDecimal precioTotal;
    private Integer cantidadAsientos;
    private String estado;
    private LocalDateTime fechaCompra;
    private List<String> asientos; // ej: ["F7", "F8"]
}
