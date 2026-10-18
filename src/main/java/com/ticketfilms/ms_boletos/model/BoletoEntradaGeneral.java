package com.ticketfilms.ms_boletos.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Entrada de un sector de admisión general (ej. "Cancha"): no hay asiento
// específico, solo una cantidad dentro del aforo del sector. sector_id es
// referencia lógica a ms-asientos.
@Entity
@Table(name = "boleto_entrada_general")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoletoEntradaGeneral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boleto_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_entradageneral_boleto"))
    private Boleto boleto;

    @Column(name = "sector_id", nullable = false)
    private Long sectorId;

    @Column(name = "sector", nullable = false, length = 60)
    private String sector;

    @Column(name = "tipo_acceso", length = 20)
    private String tipoAcceso;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;
}
