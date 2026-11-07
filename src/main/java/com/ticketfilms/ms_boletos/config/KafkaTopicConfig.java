package com.ticketfilms.ms_boletos.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    // boleto.comprado ahora viaja solo por RabbitMQ (ver RabbitTopology).
    // TODO paso 4: pasar a 3 particiones x 3 réplicas con el clúster de 3 brokers.
    public static final String COMPRA_FALLIDA = "compra.fallida";

    @Bean
    public NewTopic compraFallida() {
        return TopicBuilder.name(COMPRA_FALLIDA).partitions(1).replicas(1).build();
    }
}
