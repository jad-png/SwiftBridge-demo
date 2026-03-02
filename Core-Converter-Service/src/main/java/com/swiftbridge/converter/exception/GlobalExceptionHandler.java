package com.swiftbridge.converter.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.swiftbridge.converter.exception.FileValidationException;

import java.io.IOException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(FileValidationException.class)
    public ResponseEntity<String> handleValidation(FileValidationException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<String> handleIo(IOException e) {
        log.error("IO error: ", e);
        return ResponseEntity.internalServerError().body("Error reading file content");
    }

    public ResponseEntity<String> handleGeneral(Exception e) {
        log.error("Unexpected error: ", e);
        return ResponseEntity.internalServerError().body("An unexpected error occurred");
    }
}