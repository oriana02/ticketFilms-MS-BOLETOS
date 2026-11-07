package com.ticketfilms.ms_boletos.service.support;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.ticketfilms.ms_boletos.config.KafkaTopicConfig;
import com.ticketfilms.ms_boletos.config.RabbitTopology;
import com.ticketfilms.ms_boletos.dto.BoletoCompradoEvent;
import com.ticketfilms.ms_boletos.dto.CompraFallidaEvent;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

// Publica eventos. Reparto:
//  - boleto.comprado -> solo RabbitMQ (notificación por correo)
//  - compra.fallida  -> RabbitMQ (operativo: liberar asientos y avisar)
//                       + Kafka (log retenido de eventos, clave = funcionId)
// BoletoService ya llama a estos métodos fuera de cualquier transacción abierta
// (cada paso usa su TransactionTemplate), así que no hace falta afterCommit.
// Si un broker falla, solo se registra: la compra no se pierde por un aviso.
@Component
@RequiredArgsConstructor
public class BoletoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(BoletoEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final JsonMapper jsonMapper;

    public void boletoComprado(BoletoCompradoEvent evento) {
        publicarRabbit(RabbitTopology.RK_BOLETO_COMPRADO, evento.codigoBoleto(), evento);
    }

    public void compraFallida(CompraFallidaEvent evento) {
        publicarRabbit(RabbitTopology.RK_COMPRA_FALLIDA, evento.codigoBoleto(), evento);
        publicarKafka(KafkaTopicConfig.COMPRA_FALLIDA, String.valueOf(evento.funcionId()), evento);
    }

    private void publicarRabbit(String routingKey, String messageId, Object evento) {
        try {
            String json = jsonMapper.writeValueAsString(evento);
            Message mensaje = MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setContentEncoding("UTF-8")
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                    .setMessageId(messageId)
                    .build();
            rabbitTemplate.send(RabbitTopology.EX_EVENTOS, routingKey, mensaje);
            log.info("Evento publicado en Rabbit — routingKey={} id={}", routingKey, messageId);
        } catch (Exception e) {
            log.error("No se pudo publicar en Rabbit — routingKey={} id={} motivo={}",
                    routingKey, messageId, e.getMessage());
        }
    }

    private void publicarKafka(String topico, String clave, Object evento) {
        try {
            String json = jsonMapper.writeValueAsString(evento);
            kafkaTemplate.send(topico, clave, json).whenComplete((res, ex) -> {
                if (ex != null) {
                    log.error("No se pudo publicar en Kafka {} — clave={} motivo={}", topico, clave, ex.getMessage());
                } else {
                    log.info("Evento publicado en Kafka {} — clave={}", topico, clave);
                }
            });
        } catch (Exception e) {
            log.error("Error armando el evento para Kafka {} — {}", topico, e.getMessage());
        }
    }
}
