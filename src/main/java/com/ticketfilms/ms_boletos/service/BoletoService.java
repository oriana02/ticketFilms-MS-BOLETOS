package com.ticketfilms.ms_boletos.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ticketfilms.ms_boletos.dto.AsientoCompraDto;
import com.ticketfilms.ms_boletos.dto.BoletoResponseDto;
import com.ticketfilms.ms_boletos.dto.ConfirmarCompraRequestDto;
import com.ticketfilms.ms_boletos.model.Boleto;
import com.ticketfilms.ms_boletos.model.BoletoAsiento;
import com.ticketfilms.ms_boletos.model.CategoriaAsiento;
import com.ticketfilms.ms_boletos.model.EstadoBoleto;
import com.ticketfilms.ms_boletos.repository.BoletoRepository;
import com.ticketfilms.ms_boletos.service.support.CodigoBoletoGenerator;

import lombok.RequiredArgsConstructor;

//  NO se llama todavía a ms-asientos para marcar los
// asientos como OCUPADO: ese endpoint aún no existe en ms-asientos
// (solo está /api/asientos/reserva). Se asume que el frontend ya reservó
// los asientos antes de llegar a este paso. Ver TODO más abajo.
@Service
@RequiredArgsConstructor
public class BoletoService {

    private final BoletoRepository boletoRepository;
    private final CodigoBoletoGenerator codigoBoletoGenerator;

    @Transactional
    public BoletoResponseDto confirmarCompra(String usuarioId, ConfirmarCompraRequestDto request) {

        // TODO: cuando ms-asientos exponga el endpoint de confirmar/ocupar,
        // llamarlo ACÁ antes de persistir el boleto, y abortar la compra
        // (lanzar excepción -> 409) si algún asiento ya no está reservado
        // a nombre de este usuario.
        BigDecimal total = request.getAsientos().stream()
                .map(AsientoCompraDto::getPrecio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Boleto boleto = Boleto.builder()
                .codigoBoleto(codigoBoletoGenerator.generar())
                .usuarioId(usuarioId)
                .funcionId(request.getFuncionId())
                .eventoId(request.getEventoId())
                .tituloEvento(request.getTituloEvento())
                .fechaHoraFuncion(request.getFechaHoraFuncion())
                .precioTotal(total)
                .cantidadAsientos(request.getAsientos().size())
                .estado(EstadoBoleto.CONFIRMADO)
                .build();

        request.getAsientos().forEach(a -> boleto.agregarAsiento(
                BoletoAsiento.builder()
                        .asientoId(a.getAsientoId())
                        .fila(a.getFila())
                        .numero(a.getNumero())
                        .categoria(CategoriaAsiento.valueOf(a.getCategoria()))
                        .precioPagado(a.getPrecio())
                        .build()
        ));

        Boleto guardado = boletoRepository.save(boleto);
        return aResponseDto(guardado);
    }

    //historial de boletos del usuario autenticado
    public List<BoletoResponseDto> obtenerHistorial(String usuarioId) {
        return boletoRepository.findByUsuarioIdOrderByFechaCompraDesc(usuarioId)
                .stream()
                .map(this::aResponseDto)
                .collect(Collectors.toList());
    }

    //usado por la pantalla de confirmación / lectura de QR
    public BoletoResponseDto obtenerPorCodigo(String codigoBoleto) {
        Boleto boleto = boletoRepository.findByCodigoBoleto(codigoBoleto)
                .orElseThrow(() -> new IllegalArgumentException("Boleto no encontrado: " + codigoBoleto));
        return aResponseDto(boleto);
    }

    private BoletoResponseDto aResponseDto(Boleto boleto) {
        return BoletoResponseDto.builder()
                .codigoBoleto(boleto.getCodigoBoleto())
                .tituloEvento(boleto.getTituloEvento())
                .fechaHoraFuncion(boleto.getFechaHoraFuncion())
                .precioTotal(boleto.getPrecioTotal())
                .cantidadAsientos(boleto.getCantidadAsientos())
                .estado(boleto.getEstado().name())
                .fechaCompra(boleto.getFechaCompra())
                .asientos(boleto.getAsientos().stream()
                        .map(a -> a.getFila() + a.getNumero())
                        .collect(Collectors.toList()))
                .build();
    }
}
