package com.ticketfilms.ms_boletos.service.support;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ticketfilms.ms_boletos.exception.CategoriaInvalidaException;

// Comprueba que el precio enviado por el frontend coincida con el precio
// oficial de la categoría, para que nadie pueda comprar más barato
// modificando la solicitud.
@Component
public class PrecioValidator {

    @Value("${precios.estandar}")
    private BigDecimal precioEstandar;

    @Value("${precios.premium}")
    private BigDecimal precioPremium;

    public void validar(String categoria, BigDecimal precioEnviado) {
        if (categoria == null) {
            throw new CategoriaInvalidaException(null);
        }
        BigDecimal esperado;
        switch (categoria.trim().toUpperCase()) {
            case "ESTANDAR" -> esperado = precioEstandar;
            case "PREMIUM" -> esperado = precioPremium;
            default -> throw new CategoriaInvalidaException(categoria);
        }
        if (precioEnviado == null || precioEnviado.compareTo(esperado) != 0) {
            throw new IllegalArgumentException(
                    "Precio inválido para la categoría " + categoria
                    + ": se esperaba " + esperado + " y llegó " + precioEnviado);
        }
    }
}
