package ru.tishembitov.pictorium.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.tishembitov.pictorium.exception.*;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException exception) {
        log.warn("User not found: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        OffsetDateTime.now(),
                        "Not Found",
                        "User not found",
                        Collections.singletonList(exception.getMessage())
                ));
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUsernameAlreadyExistsException(UsernameAlreadyExistsException exception) {
        log.warn("Username already exists: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        OffsetDateTime.now(),
                        "Conflict",
                        "Username already exists",
                        Collections.singletonList(exception.getMessage())
                ));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException exception) {
        log.warn("Bad request: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        OffsetDateTime.now(),
                        "Bad Request",
                        "Invalid request",
                        Collections.singletonList(exception.getMessage())
                ));
    }

    @ExceptionHandler(SubscriptionAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleSubscriptionAlreadyExistsException(SubscriptionAlreadyExistsException exception) {
        log.warn("Subscription already exists: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        OffsetDateTime.now(),
                        "Conflict",
                        "Already following this user",
                        Collections.singletonList(exception.getMessage())
                ));
    }

    @ExceptionHandler(SubscriptionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSubscriptionNotFoundException(SubscriptionNotFoundException exception) {
        log.warn("Subscription not found: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        OffsetDateTime.now(),
                        "Not Found",
                        "Subscription not found",
                        Collections.singletonList(exception.getMessage())
                ));
    }

    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFileException(InvalidFileException exception) {
        log.warn("Invalid file: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ErrorResponse(
                        OffsetDateTime.now(),
                        "Unsupported Media Type",
                        "Invalid file",
                        Collections.singletonList(exception.getMessage())
                ));
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ErrorResponse> handleFileStorageException(FileStorageException exception) {
        log.error("File storage error: {}", exception.getMessage(), exception);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        OffsetDateTime.now(),
                        "Internal Server Error",
                        "File storage error",
                        Collections.singletonList(exception.getMessage())
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        List<String> details = exception.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        log.debug("Validation failed: {}", details);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        OffsetDateTime.now(),
                        "Bad Request",
                        "Validation failed",
                        details
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception exception) {
        log.error("Unexpected error occurred", exception);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        OffsetDateTime.now(),
                        "Internal Server Error",
                        "Unexpected error occurred",
                        Collections.singletonList("An internal error occurred. Please contact support.")
                ));
    }
}