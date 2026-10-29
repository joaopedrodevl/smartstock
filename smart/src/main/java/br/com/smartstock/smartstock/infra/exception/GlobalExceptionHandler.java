package br.com.smartstock.smartstock.infra.exception;

import br.com.smartstock.smartstock.application.exceptions.ResourceNotFoundException;
import br.com.smartstock.smartstock.application.exceptions.UserNotFoundException;
import br.com.smartstock.smartstock.core.utils.Constantes;
import br.com.smartstock.smartstock.infra.api.dtos.ErroResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Arrays;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Autowired
    private Environment environment;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponseDTO> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        log.warn("Erro de validação - uri: {}, campos: {}", request.getRequestURI(), e.getBindingResult().getErrorCount());

        String mensagem = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        var erroResponse = new ErroResponseDTO(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "ERRO_VALIDACAO",
                mensagem,
                request.getRequestURI(),
                getTraceIfDev(e)
        );

        return new ResponseEntity<>(erroResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErroResponseDTO> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        log.warn("Violação de restrição - uri: {}, violações: {}", request.getRequestURI(), e.getConstraintViolations().size());

        String mensagem = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        var erroResponse = new ErroResponseDTO(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "ERRO_VALIDACAO",
                mensagem,
                request.getRequestURI(),
                getTraceIfDev(e)
        );

        return new ResponseEntity<>(erroResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErroResponseDTO> handleResourceNotFoundException(ResourceNotFoundException e, HttpServletRequest request) {
        log.warn("Recurso nao encontrado - uri: {}, mensagem: {}", request.getRequestURI(), e.getMessage());
        var erroResponse = new ErroResponseDTO(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                "NAO_ENCONTRADO",
                e.getMessage(),
                request.getRequestURI(),
                getTraceIfDev(e)
        );

        return new ResponseEntity<>(erroResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponseDTO> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("Dado invalido/duplicado - uri: {}, mensagem: {}", request.getRequestURI(), e.getMessage());
        var erroResponse = new ErroResponseDTO(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                "DADO_DUPLICADO",
                e.getMessage(),
                request.getRequestURI(),
                getTraceIfDev(e)
        );

        return new ResponseEntity<>(erroResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErroResponseDTO> handleUserNotFoundException(UserNotFoundException e, HttpServletRequest request) {
        log.warn("Usuário não encontrado - uri: {}, mensagem: {}", request.getRequestURI(), e.getMessage());
        var erroResponse = new ErroResponseDTO(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                Constantes.USUARIO_NAO_ENCONTRADO.toString(),
                e.getMessage(),
                request.getRequestURI(),
                getTraceIfDev(e)
        );

        return new ResponseEntity<>(erroResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErroResponseDTO> handleBadCredentialsException(BadCredentialsException e, HttpServletRequest request) {
        log.warn("Credenciais inválidas - uri: {}", request.getRequestURI());
        var erroResponse = new ErroResponseDTO(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "CREDENCIAIS_INVALIDAS",
                e.getMessage(),
                request.getRequestURI(),
                getTraceIfDev(e)
        );

        return new ResponseEntity<>(erroResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponseDTO> handleException(Exception e, HttpServletRequest request) {
        log.error("Erro interno - uri: {}, mensagem: {}", request.getRequestURI(), e.getMessage(), e);
        var erroResponse = new ErroResponseDTO(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "ERRO_INTERNO",
                e.getMessage(),
                request.getRequestURI(),
                getTraceIfDev(e)
        );

        return new ResponseEntity<>(erroResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String getTraceIfDev(Exception e) {
        if (!isDevMode()) {
            return null;
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    private boolean isDevMode() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }
}
