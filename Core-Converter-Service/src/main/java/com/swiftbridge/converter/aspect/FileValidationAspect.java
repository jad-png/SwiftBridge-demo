package com.swiftbridge.converter.aspect;

import com.swiftbridge.converter.aspect.annotation.ValidXmlFile;
import com.swiftbridge.converter.exception.FileValidationException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Aspect
@Component
@Slf4j
public class FileValidationAspect {

    @Before("@annotation(validXmlFile)")
    public void validate(JoinPoint joinPoint, ValidXmlFile validXmlFile) {
        Object[] args = joinPoint.getArgs();

        for (Object arg : args) {
            if (arg instanceof MultipartFile file) {
                if (file.isEmpty()) throw new FileValidationException("File is empty");

                if (file.getSize() > validXmlFile.maxSize())
                    throw new FileValidationException("File size exceeds limit");

                String filename = file.getOriginalFilename();
                if (filename == null || !filename.toLowerCase().endsWith(".xml"))
                    throw new FileValidationException("Only XML files are allowed");

                log.info("AOP: File {} validated successfully", filename);

            }
        }
    }
}
