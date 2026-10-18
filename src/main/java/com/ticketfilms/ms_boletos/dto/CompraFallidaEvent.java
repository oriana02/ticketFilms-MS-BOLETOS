package com.ticketfilms.ms_boletos.dto;

import java.util.List;

// Lo leen dos consumidores con el mismo mensaje:
//  - ms-notificaciones usa codigoBoleto, emailUsuario y motivo (avisa al usuario)
//  - ms-asientos usa funcionId, asientosIds y usuarioId (libera los asientos)
public record CompraFallidaEvent(
        String codigoBoleto,
        String emailUsuario,
        String motivo,
        Long funcionId,
        List<Long> asientosIds,
        String usuarioId
) {}
