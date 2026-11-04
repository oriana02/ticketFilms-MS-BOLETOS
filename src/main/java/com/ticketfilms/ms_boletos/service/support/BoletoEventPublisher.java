package com.ticketfilms.ms_boletos.service.support;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.ticketfilms.ms_boletos.config.RabbitTopology;
import com.ticketfilms.ms_boletos.dto.BoletoCompradoEvent;
import com.ticketfilms.ms_boletos.dto.CompraFallidaEvent;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

// Publica eventos en RabbitMQ (exchange topic ex.tickets.eventos). Si RabbitMQ falla,
// solo se registra en el log: el boleto ya está confirmado y no debe perderse la compra
// por un aviso.
@Component
@RequiredArgsConstructor
public class BoletoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(BoletoEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final JsonMapper jsonMapper;

    public void boletoComprado(BoletoCompradoEvent evento) {
        publicar(RabbitTopology.RK_BOLETO_COMPRADO, evento.codigoBoleto(), evento);
    }

    public void compraFallida(CompraFallidaEvent evento) {
        publicar(RabbitTopology.RK_COMPRA_FALLIDA, evento.codigoBoleto(), evento);
    }

    private void publicar(String routingKey, String id, Object evento) {
        try {
            String json = jsonMapper.writeValueAsString(evento);
            Message mensaje = MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setContentEncoding("UTF-8")
                    .setMessageId(id)
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                    .build();
            rabbitTemplate.send(RabbitTopology.EX_EVENTOS, routingKey, mensaje);
            log.info("Evento publicado — exchange={} routingKey={} id={}",
                    RabbitTopology.EX_EVENTOS, routingKey, id);
        } catch (Exception e) {
            log.error("No se pudo publicar en {} — routingKey={} id={} motivo={}",
                    RabbitTopology.EX_EVENTOS, routingKey, id, e.getMessage());
        }
    }
}
