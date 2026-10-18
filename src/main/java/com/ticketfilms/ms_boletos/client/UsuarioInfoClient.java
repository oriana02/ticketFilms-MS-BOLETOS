package com.ticketfilms.ms_boletos.client;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// El access token de Cognito no trae el email; el endpoint userInfo sí
// (requiere que el login haya pedido los scopes "openid email").
// Si falla, devuelve null: la compra no debe caerse por no poder avisar.
@Slf4j
@Component
@RequiredArgsConstructor
public class UsuarioInfoClient {

    private final RestClient cognitoRestClient;

    @Value("${cognito.userinfo-url}")
    private String userInfoUrl;

    public String obtenerEmail(String bearerToken) {
        try {
            Map<?, ?> info = cognitoRestClient.get()
                    .uri(userInfoUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                    .retrieve()
                    .body(Map.class);
            Object email = info != null ? info.get("email") : null;
            return email != null ? email.toString() : null;
        } catch (Exception e) {
            log.warn("No se pudo obtener el email desde Cognito: {}", e.getMessage());
            return null;
        }
    }
}
