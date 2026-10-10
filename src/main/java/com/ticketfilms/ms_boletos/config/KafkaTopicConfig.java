package com.ticketfilms.ms_boletos.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String BOLETO_COMPRADO = "boleto.comprado";
    public static final String COMPRA_FALLIDA = "compra.fallida";

    @Bean
    public NewTopic boletoComprado() {
        return TopicBuilder.name(BOLETO_COMPRADO).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic compraFallida() {
        return TopicBuilder.name(COMPRA_FALLIDA).partitions(1).replicas(1).build();
    }
}
