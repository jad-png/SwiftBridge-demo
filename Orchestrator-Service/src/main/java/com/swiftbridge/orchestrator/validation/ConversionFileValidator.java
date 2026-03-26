package com.swiftbridge.orchestrator.validation;

import com.swiftbridge.orchestrator.exception.ValidationFailedException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ConversionFileValidator {
    private static final long MAX_FILE_SIZE = 10_000_000L;

    public void validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ValidationFailedException("ERR_EMPTY_FILE", "File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ValidationFailedException("ERR_FILE_TOO_LARGE", "File size exceeds 10MB limit");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xml")) {
            throw new ValidationFailedException("ERR_FILE_TYPE", "Only XML files are accepted");
        }
    }
}
