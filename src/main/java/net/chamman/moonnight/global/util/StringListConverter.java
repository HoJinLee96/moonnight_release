package net.chamman.moonnight.global.util;

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
		try {
			return objectMapper.writeValueAsString(attribute);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("JSON 변환 오류", e);
		}
	}
	
	@Override
	public List<String> convertToEntityAttribute(String dbData) {
		try {
			return objectMapper.readValue(dbData, new TypeReference<List<String>>() {});
		} catch (Exception e) {
			throw new RuntimeException("JSON 파싱 오류", e);
		}
	}
}