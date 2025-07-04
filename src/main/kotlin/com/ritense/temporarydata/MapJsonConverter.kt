package com.ritense.temporarydata

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.AttributeConverter
import org.springframework.util.StringUtils

class MapJsonConverter(
    private val mapper: ObjectMapper
) : AttributeConverter<Map<String, Any>, String> {
    override fun convertToDatabaseColumn(map: Map<String, Any>): String {
        return mapper.writeValueAsString(map);
    }

    override fun convertToEntityAttribute(json: String): MutableMap<String, Any> {
        if (!StringUtils.hasText(json)) {
            return mutableMapOf()
        }
        val typeRef = object : TypeReference<MutableMap<String, Any>>() {}
        return mapper.readValue(json, typeRef)
    }
}
