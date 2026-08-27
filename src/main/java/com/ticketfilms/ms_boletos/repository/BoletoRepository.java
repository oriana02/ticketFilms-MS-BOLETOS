package com.ticketfilms.ms_boletos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ticketfilms.ms_boletos.model.Boleto;
import com.ticketfilms.ms_boletos.model.EstadoBoleto;

public interface BoletoRepository extends JpaRepository<Boleto, Long> {

    //historial de boletos del usuario autenticado (usuarioId = sub/email del JWT)
    @EntityGraph(attributePaths = "asientos")
    List<Boleto> findByUsuarioIdOrderByFechaCompraDesc(String usuarioId);

    //pantalla de confirmación de compra / lectura de QR (código público del boleto)
    @EntityGraph(attributePaths = "asientos")
    Optional<Boleto> findByCodigoBoleto(String codigoBoleto);

    //usado al generar el código aleatorio, para reintentar si ya existe
    boolean existsByCodigoBoleto(String codigoBoleto);

    //util para reportes: todos los boletos de una función puntual
    List<Boleto> findByFuncionId(Long funcionId);

    //historial filtrado por estado (ej. solo CONFIRMADO, ignorando ANULADO)
    List<Boleto> findByUsuarioIdAndEstadoOrderByFechaCompraDesc(String usuarioId, EstadoBoleto estado);
}
