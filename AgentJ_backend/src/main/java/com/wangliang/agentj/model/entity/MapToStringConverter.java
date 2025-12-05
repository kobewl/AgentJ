/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wangliang.agentj.model.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author dahua
 * @since 2025/7/12 13:02
 */
@Converter
public class MapToStringConverter implements AttributeConverter<Map<String, String>, String> {

	private static final Logger log = LoggerFactory.getLogger(MapToStringConverter.class);

	private final ObjectMapper objectMapper;

	public MapToStringConverter() {
		// Fallback ObjectMapper to keep converter usable even if dependency injection
		// does not kick in.
		this.objectMapper = new ObjectMapper();
	}

	public MapToStringConverter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public String convertToDatabaseColumn(Map<String, String> attribute) {
		try {
			if (attribute == null || attribute.isEmpty()) {
				return "{}";
			}
			return objectMapper.writeValueAsString(attribute);
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Error converting map to string", e);
		}
	}

	@Override
	public Map<String, String> convertToEntityAttribute(String dbData) {
		if (!StringUtils.hasText(dbData) || "{}".equals(dbData.trim())) {
			return new HashMap<>();
		}
		try {
			return objectMapper.readValue(dbData, new TypeReference<>() {
			});
		}
		catch (Exception e) {
			// Be forgiving with malformed data to avoid breaking the app
			log.warn("Failed to parse headers JSON from DB, returning empty map. Raw: {}", dbData, e);
			return new HashMap<>();
		}
	}

}
