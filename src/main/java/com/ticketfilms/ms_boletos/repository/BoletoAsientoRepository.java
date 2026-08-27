package com.ticketfilms.ms_boletos.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ticketfilms.ms_boletos.model.BoletoAsiento;

public interface BoletoAsientoRepository extends JpaRepository<BoletoAsiento, Long> {

    List<BoletoAsiento> findByBoletoId(Long boletoId);

    List<BoletoAsiento> findByAsientoId(Long asientoId);
}
