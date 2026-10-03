package com.uphill.common.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.common.KafkaException;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({BusinessException.class})
    public ResponseEntity<Object> handleBusinessException(BusinessException exception, WebRequest request) {
        ProblemDetail error = buildResponse(exception, exception.getStatus(), null);
        return this.handleExceptionInternal(exception, error, new HttpHeaders(), exception.getStatus(), request);
    }

    @ExceptionHandler({DataIntegrityViolationException.class})
    public ResponseEntity<Object> handleDataAccessException(DataIntegrityViolationException exception, WebRequest request) {
        ProblemDetail error = buildResponse(ErrorMessage.TRANSACTION_FAILED, HttpStatus.CONFLICT, null);
        return this.handleExceptionInternal(exception, error, new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler({ConstraintViolationException.class})
    public ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException exception, WebRequest request) {
        ProblemDetail error = buildResponse(exception, HttpStatus.BAD_REQUEST, exception.getMessage());
        return this.handleExceptionInternal(exception, error, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler({KafkaException.class})
    public ResponseEntity<Object> handleKafkaException(KafkaException exception, WebRequest request) {
        ProblemDetail error = buildResponse(ErrorMessage.TRANSACTION_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, null);
        return this.handleExceptionInternal(exception, error, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<Object> handle(Exception exception, WebRequest request) {
        ProblemDetail error = buildResponse(exception, HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
        return this.handleExceptionInternal(exception, error, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail body = buildResponse(ex, HttpStatus.BAD_REQUEST, ErrorMessage.INVALID_VALUES);
        return this.handleExceptionInternal(ex, body, headers, status, request);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<String> errors = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map((error) -> error.getObjectName() + "." + error.getField() + ": " + error.getDefaultMessage()).collect(Collectors.toList());

        ProblemDetail error = buildResponse(exception, HttpStatus.BAD_REQUEST, String.join(", ", errors));

        return this.handleExceptionInternal(exception, error, new HttpHeaders(), status, request);
    }

    private ProblemDetail buildResponse(String exception, HttpStatus status, String errorMessage) {
        return ProblemDetail.forStatusAndDetail(status, StringUtils.defaultIfBlank(errorMessage, exception));
    }

    private ProblemDetail buildResponse(Exception exception, HttpStatus status, String errorMessage) {
        return ProblemDetail.forStatusAndDetail(status, StringUtils.defaultIfBlank(errorMessage, exception.getMessage()));
    }

    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {

        Throwable rootCause = ExceptionUtils.getRootCause(exception);

        if (CollectionUtils.isEmpty(List.of(rootCause.getStackTrace()))) {
            log.error("{}", rootCause.getMessage());
        } else {
            StackTraceElement stackTraceElement = rootCause.getStackTrace()[0];
            log.error("{}:{} - {} - {}", stackTraceElement.getClassName(), stackTraceElement.getLineNumber(), stackTraceElement.getMethodName(), rootCause.getMessage());
        }

        log.debug("", exception);

        return new ResponseEntity<>(body, statusCode);
    }
}
