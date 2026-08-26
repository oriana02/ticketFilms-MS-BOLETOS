package com.ticketfilms.ms_boletos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Mapea 1:1 la tabla `boleto` de ms-boletos-schema.sql (RF-10, RF-11, RF-12).
// funcion_id y evento_id son referencias LÓGICAS a ms-cartelera (sin FK real
// entre microservicios, ver notas del schema). titulo_evento y
// fecha_hora_funcion viven denormalizados acá para que "Mis boletos" (RF-12)
// no dependa de que ms-cartelera siga teniendo esos datos vivos.
@Entity
@Table(
        name = "boleto",
        uniqueConstraints = @UniqueConstraint(name = "uq_boleto_codigo", columnNames = "codigo_boleto"),
        indexes = {
            @Index(name = "idx_boleto_usuario", columnList = "usuario_id"),
            @Index(name = "idx_boleto_funcion", columnList = "funcion_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Boleto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_boleto", nullable = false, length = 20)
    private String codigoBoleto;

    // sub / email del JWT validado por el Resource Server (RF-02/RF-14)
    @Column(name = "usuario_id", nullable = false, length = 255)
    private String usuarioId;

    @Column(name = "funcion_id", nullable = false)
    private Long funcionId;

    @Column(name = "evento_id", nullable = false)
    private Long eventoId;

    @Column(name = "titulo_evento", nullable = false, length = 255)
    private String tituloEvento;

    @Column(name = "fecha_hora_funcion", nullable = false)
    private LocalDateTime fechaHoraFuncion;

    @Column(name = "precio_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioTotal;

    @Column(name = "cantidad_asientos", nullable = false)
    private Integer cantidadAsientos;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoBoleto estado = EstadoBoleto.CONFIRMADO;

    @Column(name = "fecha_compra", nullable = false)
    @Builder.Default
    private LocalDateTime fechaCompra = LocalDateTime.now();

    @OneToMany(mappedBy = "boleto", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BoletoAsiento> asientos = new ArrayList<>();

    public void agregarAsiento(BoletoAsiento asiento) {
        asiento.setBoleto(this);
        this.asientos.add(asiento);
    }
}
