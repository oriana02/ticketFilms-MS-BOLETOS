package com.ticketfilms.ms_boletos.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

// boleto.comprado viaja solo por RabbitMQ (ver RabbitTopology).
// compra.fallida: misma configuración que declara ms-asientos (3x3, min ISR 2,
// retención 7 días). Kafka NO modifica un tópico que ya existe, así que si
// ambos servicios lo declaran, deben coincidir.
@Configuration
public class KafkaTopicConfig {

    public static final String COMPRA_FALLIDA = "compra.fallida";
    private static final String RETENCION_NEGOCIO_MS = "604800000";

    @Bean
    public NewTopic compraFallida() {
        return TopicBuilder.name(COMPRA_FALLIDA)
                .partitions(3).replicas(3)
                .config("min.insync.replicas", "2")
                .config("retention.ms", RETENCION_NEGOCIO_MS)
                .build();
    }
}
