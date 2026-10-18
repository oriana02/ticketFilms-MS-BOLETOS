package com.ticketfilms.ms_boletos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BoletoCompradoEvent(
        String codigoBoleto,
        String emailUsuario,
        String tituloEvento,
        LocalDateTime fechaHoraFuncion,
        BigDecimal precioTotal,
        List<String> asientos
        ) {}
