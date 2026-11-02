package com.example.appeveapigateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.UncheckedIOException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GatewayExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayExceptionHandler.class);

    @ExceptionHandler({HttpServerErrorException.class, ResourceAccessException.class, UncheckedIOException.class})
    public ResponseEntity<GatewayError> handleServiceUnavailable(Exception ex) {
        log.error("Error de comunicación con un servicio downstream", ex);
        return error(HttpStatus.SERVICE_UNAVAILABLE, "El servicio consultado no se encuentra disponible.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<GatewayError> handleNotFound(NoResourceFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "El recurso solicitado no se encuentra disponible.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GatewayError> handleUnexpected(Exception ex) {
        log.error("Error no controlado en el gateway", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Intente nuevamente más tarde.");
    }

    private ResponseEntity<GatewayError> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(GatewayError.of(message));
    }
}
