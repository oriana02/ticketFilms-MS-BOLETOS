package com.ticketfilms.ms_boletos.service.support;

import com.ticketfilms.ms_boletos.repository.BoletoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

// genera el código público del boleto (ej. TF-AB12-CD34), usado en la
// pantalla de confirmación y en el QR. Aislado del BoletoService
// para poder testearlo solo y para poder cambiar el formato sin tocar
// la lógica de negocio de la compra
@Component
@RequiredArgsConstructor
public class CodigoBoletoGenerator {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // sin 0/O/1/I
    private static final int GROUP_LENGTH = 4;
    private static final int MAX_INTENTOS = 5;

    private final SecureRandom random = new SecureRandom();
    private final BoletoRepository boletoRepository;

    public String generar() {
        for (int intento = 0; intento < MAX_INTENTOS; intento++) {
            String candidato = construirCodigo();
            if (!boletoRepository.existsByCodigoBoleto(candidato)) {
                return candidato;
            }
        }
        throw new IllegalStateException(
                "No se pudo generar un código de boleto único tras " + MAX_INTENTOS + " intentos"
        );
    }

    private String construirCodigo() {
        String grupo1 = randomGroup();
        String grupo2 = randomGroup();
        return "TF-" + grupo1 + "-" + grupo2;
    }

    private String randomGroup() {
        StringBuilder sb = new StringBuilder(GROUP_LENGTH);
        for (int i = 0; i < GROUP_LENGTH; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
