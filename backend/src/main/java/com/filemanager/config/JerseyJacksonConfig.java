package com.filemanager.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

/**
 * Cấu hình Jackson cho Jersey — hỗ trợ serialize LocalDateTime khi trả JSON (upload, file list...).
 */
@Provider
public class JerseyJacksonConfig implements ContextResolver<ObjectMapper> {

    private final ObjectMapper mapper;

    public JerseyJacksonConfig() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }
}
