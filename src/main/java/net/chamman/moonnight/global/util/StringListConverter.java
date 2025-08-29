package net.chamman.moonnight.global.util;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {
	
	private static final ObjectMapper objectMapper = new ObjectMapper();
	
	@Override
	public String convertToDatabaseColumn(List<String> attribute) {
		if (attribute == null || attribute.isEmpty()) {
            return ""; 
        }
		try {
			return objectMapper.writeValueAsString(attribute);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("JSON 변환 오류", e);
		}
	}
	
	@Override
	public List<String> convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.isBlank()) {
            return Collections.emptyList(); 
        }
		try {
			return objectMapper.readValue(dbData, new TypeReference<List<String>>() {});
		} catch (Exception e) {
			throw new RuntimeException("JSON 파싱 오류", e);
		}
	}
}