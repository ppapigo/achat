package com.achat.augment.effect;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;

@Converter
public class MessageEffectsConverter implements AttributeConverter<List<MessageEffect>, String> {
    private static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();
    @Override public String convertToDatabaseColumn(List<MessageEffect> effects) {
        try { return MAPPER.writeValueAsString(effects == null ? List.of() : effects); }
        catch (Exception e) { throw new IllegalArgumentException("효과 저장 실패", e); }
    }
    @Override public List<MessageEffect> convertToEntityAttribute(String value) {
        if (value == null || value.isBlank()) return List.of();
        try { return List.copyOf(MAPPER.readValue(value, new TypeReference<List<MessageEffect>>() {})); }
        catch (Exception e) { throw new IllegalArgumentException("효과 읽기 실패", e); }
    }
}
