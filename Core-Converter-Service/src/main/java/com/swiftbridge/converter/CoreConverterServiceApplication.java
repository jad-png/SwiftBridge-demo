package com.swiftbridge.converter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.swiftbridge.converter"})
public class CoreConverterServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreConverterServiceApplication.class, args);
    }

}
