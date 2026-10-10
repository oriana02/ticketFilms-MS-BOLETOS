package com.ticketfilms.ms_boletos.dto;

public record CompraFallidaEvent(String codigoBoleto, String emailUsuario, String motivo) {}

