package com.ticketfilms.ms_boletos.service.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.ticketfilms.ms_boletos.config.KafkaTopicConfig;
import com.ticketfilms.ms_boletos.dto.BoletoCompradoEvent;
import com.ticketfilms.ms_boletos.dto.CompraFallidaEvent;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

// Publica eventos en Kafka. Si Kafka falla, solo se registra en el log:
// el boleto ya está confirmado y no debe perderse la compra por un aviso.
@Component
@RequiredArgsConstructor
public class BoletoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(BoletoEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    public void boletoComprado(BoletoCompradoEvent evento) {
        publicar(KafkaTopicConfig.BOLETO_COMPRADO, evento.codigoBoleto(), evento);
    }

    public void compraFallida(CompraFallidaEvent evento) {
        publicar(KafkaTopicConfig.COMPRA_FALLIDA, evento.codigoBoleto(), evento);
    }

    private void publicar(String topico, String clave, Object evento) {
        try {
            String json = jsonMapper.writeValueAsString(evento);
            kafkaTemplate.send(topico, clave, json).whenComplete((res, ex) -> {
                if (ex != null) {
                    log.error("No se pudo publicar en {} — clave={} motivo={}", topico, clave, ex.getMessage());
                } else {
                    log.info("Evento publicado en {} — clave={}", topico, clave);
                }
            });
        } catch (Exception e) {
            log.error("Error armando el evento para {} — {}", topico, e.getMessage());
        }
    }
}
