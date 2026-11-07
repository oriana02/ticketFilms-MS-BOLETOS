package com.ticketfilms.ms_boletos.service.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import com.ticketfilms.ms_boletos.config.KafkaTopicConfig;
import com.ticketfilms.ms_boletos.config.RabbitTopology;
import com.ticketfilms.ms_boletos.dto.BoletoCompradoEvent;
import com.ticketfilms.ms_boletos.dto.CompraFallidaEvent;

import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class BoletoEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private BoletoEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new BoletoEventPublisher(kafkaTemplate, rabbitTemplate, JsonMapper.builder().build());
    }

    private String cuerpoRabbit(String routingKey) {
        ArgumentCaptor<Message> msg = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq(RabbitTopology.EX_EVENTOS), eq(routingKey), msg.capture());
        return new String(msg.getValue().getBody(), StandardCharsets.UTF_8);
    }

    @Test
    void boletoComprado_vaSoloPorRabbit() {
        publisher.boletoComprado(new BoletoCompradoEvent(
                "TF-AB12-CD34", "a@b.cl", "Dune", LocalDateTime.of(2026, 12, 1, 20, 0),
                new BigDecimal("11000"), List.of("F7", "F8")));

        String json = cuerpoRabbit(RabbitTopology.RK_BOLETO_COMPRADO);
        assertTrue(json.contains("\"emailUsuario\":\"a@b.cl\""));
        assertTrue(json.contains("TF-AB12-CD34"));
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void compraFallida_vaPorRabbitYPorKafkaConFuncionIdComoClave() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.compraFallida(new CompraFallidaEvent(
                "TF-AB12-CD34", "a@b.cl", "La reserva expiró", 9L, List.of(7L, 8L), "sub-123"));

        String json = cuerpoRabbit(RabbitTopology.RK_COMPRA_FALLIDA);
        assertTrue(json.contains("\"usuarioId\":\"sub-123\""));
        assertTrue(json.contains("\"emailUsuario\":\"a@b.cl\""));

        ArgumentCaptor<String> clave = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(KafkaTopicConfig.COMPRA_FALLIDA), clave.capture(), anyString());
        assertEquals("9", clave.getValue());
    }
}
