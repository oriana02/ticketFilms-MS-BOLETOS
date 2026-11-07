package com.ticketfilms.ms_boletos.config;

import java.util.List;

// Único lugar donde se escriben los nombres de exchanges, colas y routing keys.
// DEBE ser idéntico al de ms-asientos: si los argumentos de una cola difieren,
// RabbitMQ rechaza la redeclaración (PRECONDITION_FAILED).
public final class RabbitTopology {

    private RabbitTopology() {
    }

    // Exchanges
    public static final String EX_EVENTOS = "ex.tickets.eventos"; // tipo topic
    public static final String EX_DLX = "ex.tickets.dlx";         // tipo direct (dead letter)

    // Routing keys (patrón <entidad>.<evento>)
    public static final String RK_BOLETO_COMPRADO = "boleto.comprado";
    public static final String RK_COMPRA_FALLIDA = "compra.fallida";
    public static final String RK_RESERVA_EXPIRADA = "reserva.expirada";

    public static final String SUFIJO_DLQ = ".dlq";

    // Colas funcionales
    public static final String Q_BOLETO_COMPRADO = "q.boleto.comprado.notificaciones";
    public static final String Q_COMPRA_FALLIDA_NOTIFICACIONES = "q.compra.fallida.notificaciones";
    public static final String Q_COMPRA_FALLIDA_ASIENTOS = "q.compra.fallida.asientos";
    public static final String Q_RESERVA_EXPIRADA = "q.reserva.expirada.notificaciones";

    public static final List<String> COLAS = List.of(
            Q_BOLETO_COMPRADO,
            Q_COMPRA_FALLIDA_NOTIFICACIONES,
            Q_COMPRA_FALLIDA_ASIENTOS,
            Q_RESERVA_EXPIRADA);

    public static String dlq(String cola) {
        return cola + SUFIJO_DLQ;
    }
}
