package com.ticketfilms.ms_boletos.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    // URL base de ms-asientos
    @Value("${ms-asientos.base-url}")
    private String msAsientosBaseUrl;

    @Bean
    public RestClient asientosRestClient() {
        return RestClient.builder()
                .baseUrl(msAsientosBaseUrl)
                .build();
    }

    // Para consultar el email del usuario a Cognito (userInfo).
    // Tiempo máximo corto: si Cognito se demora, la compra no debe esperar.
    @Bean
    public RestClient cognitoRestClient() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
