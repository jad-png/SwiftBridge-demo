package com.swiftbridge.converter.service;

import com.swiftbridge.converter.mapping.model.mt103.Mt103Message;
import com.swiftbridge.converter.mapping.mt103.builder.MT103Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Mt103SerializationService {

    private final MT103Builder mt103Builder;

    public String serialize(Mt103Message message) {
        return mt103Builder.build(message);
    }
}
