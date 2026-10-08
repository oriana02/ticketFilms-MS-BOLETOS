package com.ticketfilms.ms_boletos.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.ticketfilms.ms_boletos.client.AsientosClient;
import com.ticketfilms.ms_boletos.dto.AsientoCompraDto;
import com.ticketfilms.ms_boletos.dto.BoletoResponseDto;
import com.ticketfilms.ms_boletos.dto.ConfirmarCompraRequestDto;
import com.ticketfilms.ms_boletos.dto.EntradaGeneralDto;
import com.ticketfilms.ms_boletos.exception.BoletoNoEncontradoException;
import com.ticketfilms.ms_boletos.exception.CategoriaInvalidaException;
import com.ticketfilms.ms_boletos.model.Boleto;
import com.ticketfilms.ms_boletos.model.BoletoAsiento;
import com.ticketfilms.ms_boletos.model.BoletoEntradaGeneral;
import com.ticketfilms.ms_boletos.model.CategoriaAsiento;
import com.ticketfilms.ms_boletos.model.EstadoBoleto;
import com.ticketfilms.ms_boletos.repository.BoletoRepository;
import com.ticketfilms.ms_boletos.service.support.CodigoBoletoGenerator;
import com.ticketfilms.ms_boletos.service.support.PrecioValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BoletoService {

    private final BoletoRepository boletoRepository;
    private final CodigoBoletoGenerator codigoBoletoGenerator;
    private final AsientosClient asientosClient;
    private final TransactionTemplate transactionTemplate;
    private final PrecioValidator precioValidator;

    // Sin @Transactional a propósito: cada paso usa su propia transacción
    // (TransactionTemplate) para que el estado del boleto quede guardado
    // aunque falle un paso posterior.
    public BoletoResponseDto confirmarCompra(String usuarioId, String bearerToken, ConfirmarCompraRequestDto request) {

        List<AsientoCompraDto> asientosDto
                = request.getAsientos() != null ? request.getAsientos() : new ArrayList<>();
        List<EntradaGeneralDto> generalesDto
                = request.getEntradasGenerales() != null ? request.getEntradasGenerales() : new ArrayList<>();

        if (asientosDto.isEmpty() && generalesDto.isEmpty()) {
            throw new IllegalArgumentException("La compra debe incluir al menos un asiento o una entrada");
        }

        List<Long> asientoIds = asientosDto.stream()
                .map(AsientoCompraDto::getAsientoId)
                .collect(Collectors.toList());

        // Paso 1: construir (valida categorías y precios) y guardar como PENDIENTE
        Boleto boleto = construirBoleto(usuarioId, request, asientosDto, generalesDto);
        String codigo = boleto.getCodigoBoleto();
        transactionTemplate.execute(status -> boletoRepository.save(boleto));

        // Paso 2: confirmar en ms-asientos
        try {
            if (!asientoIds.isEmpty()) {
                asientosClient.confirmarAsientos(bearerToken, request.getFuncionId(), asientoIds);
            }
            // TODO: confirmar las entradas generales (descontar del aforo) cuando
            // ms-asientos tenga ese endpoint. Se llamaría aquí con generalesDto.
        } catch (RuntimeException e) {
            cambiarEstado(codigo, EstadoBoleto.CANCELADO);
            throw e;
        }

        // Paso 3: marcar CONFIRMADO. Si esto fallara, el boleto queda PENDIENTE
        // con los asientos ya OCUPADO: es detectable y corregible.
        return transactionTemplate.execute(status -> {
            Boleto b = boletoRepository.findByCodigoBoleto(codigo).orElseThrow();
            b.setEstado(EstadoBoleto.CONFIRMADO);
            return aResponseDto(boletoRepository.save(b));
        });
    }

    private Boleto construirBoleto(String usuarioId, ConfirmarCompraRequestDto request,
            List<AsientoCompraDto> asientosDto,
            List<EntradaGeneralDto> generalesDto) {
        asientosDto.forEach(a -> precioValidator.validar(a.getCategoria(), a.getPrecio()));

        BigDecimal totalAsientos = asientosDto.stream()
                .map(AsientoCompraDto::getPrecio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalGenerales = generalesDto.stream()
                .map(g -> g.getPrecioUnitario().multiply(BigDecimal.valueOf(g.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int cantidadGenerales = generalesDto.stream()
                .mapToInt(EntradaGeneralDto::getCantidad)
                .sum();

        Boleto boleto = Boleto.builder()
                .codigoBoleto(codigoBoletoGenerator.generar())
                .usuarioId(usuarioId)
                .funcionId(request.getFuncionId())
                .eventoId(request.getEventoId())
                .tituloEvento(request.getTituloEvento())
                .fechaHoraFuncion(request.getFechaHoraFuncion())
                .tipoEvento(request.getTipoEvento())
                .sede(request.getSede())
                .ciudad(request.getCiudad())
                .puerta(request.getPuerta())
                .precioTotal(totalAsientos.add(totalGenerales))
                .cantidadAsientos(asientosDto.size() + cantidadGenerales)
                .estado(EstadoBoleto.PENDIENTE)
                .build();

        asientosDto.forEach(a -> boleto.agregarAsiento(
                BoletoAsiento.builder()
                        .asientoId(a.getAsientoId())
                        .fila(a.getFila())
                        .numero(a.getNumero())
                        .categoria(parsearCategoria(a.getCategoria()))
                        .precioPagado(a.getPrecio())
                        .sector(a.getSector())
                        .tipoAcceso(a.getTipoAcceso())
                        .build()
        ));

        generalesDto.forEach(g -> boleto.agregarEntradaGeneral(
                BoletoEntradaGeneral.builder()
                        .sectorId(g.getSectorId())
                        .sector(g.getSector())
                        .tipoAcceso(g.getTipoAcceso())
                        .cantidad(g.getCantidad())
                        .precioUnitario(g.getPrecioUnitario())
                        .build()
        ));
        return boleto;
    }

    private CategoriaAsiento parsearCategoria(String categoria) {
        if (categoria == null) {
            throw new CategoriaInvalidaException(null);
        }
        try {
            return CategoriaAsiento.valueOf(categoria.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CategoriaInvalidaException(categoria);
        }
    }

    private void cambiarEstado(String codigo, EstadoBoleto nuevoEstado) {
        transactionTemplate.executeWithoutResult(status
                -> boletoRepository.findByCodigoBoleto(codigo).ifPresent(b -> {
                    b.setEstado(nuevoEstado);
                    boletoRepository.save(b);
                }));
    }

    public List<BoletoResponseDto> obtenerHistorial(String usuarioId) {
        return boletoRepository.findByUsuarioIdOrderByFechaCompraDesc(usuarioId)
                .stream()
                .filter(b -> b.getEstado() == EstadoBoleto.CONFIRMADO)
                .map(this::aResponseDto)
                .collect(Collectors.toList());
    }

    // Solo el dueño puede ver su boleto. Si no existe o es de otro usuario,
    // la respuesta es la misma (404) para no revelar qué códigos existen.
    public BoletoResponseDto obtenerPorCodigo(String usuarioId, String codigoBoleto) {
        Boleto boleto = boletoRepository.findByCodigoBoleto(codigoBoleto)
                .filter(b -> usuarioId.equals(b.getUsuarioId()))
                .orElseThrow(() -> new BoletoNoEncontradoException(codigoBoleto));
        return aResponseDto(boleto);
    }

    private BoletoResponseDto aResponseDto(Boleto boleto) {
        List<String> entradas = boleto.getAsientos().stream()
                .map(a -> a.getFila() + a.getNumero())
                .collect(Collectors.toList());
        boleto.getEntradasGenerales().forEach(g
                -> entradas.add(g.getSector() + " x" + g.getCantidad()));

        return BoletoResponseDto.builder()
                .codigoBoleto(boleto.getCodigoBoleto())
                .tituloEvento(boleto.getTituloEvento())
                .fechaHoraFuncion(boleto.getFechaHoraFuncion())
                .tipoEvento(boleto.getTipoEvento())
                .sede(boleto.getSede())
                .ciudad(boleto.getCiudad())
                .puerta(boleto.getPuerta())
                .precioTotal(boleto.getPrecioTotal())
                .cantidadAsientos(boleto.getCantidadAsientos())
                .estado(boleto.getEstado().name())
                .fechaCompra(boleto.getFechaCompra())
                .asientos(entradas)
                .build();
    }
}
