package com.ticketfilms.ms_boletos.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

// Traduce excepciones de negocio a códigos HTTP correctos, en vez de
// dejar que Spring devuelva 500 genérico para todo. RNF-06: mantiene la
// lógica de errores centralizada y fuera de los controllers.
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Ej: BoletoService.obtenerPorCodigo cuando el código no existe
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(cuerpoError(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    // Ej: CodigoBoletoGenerator si agota los reintentos
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(cuerpoError(HttpStatus.CONFLICT, ex.getMessage()));
    }

    // Errores de @Valid en el body del request (ej. ConfirmarCompraRequestDto incompleto)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(cuerpoError(HttpStatus.BAD_REQUEST, detalle));
    }

    private Map<String, Object> cuerpoError(HttpStatus status, String mensaje) {
        return Map.of(
            "timestamp", LocalDateTime.now().toString(),
            "status", status.value(),
            "error", status.getReasonPhrase(),
            "mensaje", mensaje
        );
    }
}
