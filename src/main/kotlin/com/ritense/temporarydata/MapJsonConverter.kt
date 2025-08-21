package com.ritense.temporarydata

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.AttributeConverter
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies

class MapJsonConverter(
    private val mapper: ObjectMapper
) : AttributeConverter<Map<String, Any?>, String> {

    init {
        // Configure ObjectMapper for better handling of nested objects
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        mapper.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
    }

    override fun convertToDatabaseColumn(map: Map<String, Any?>?): String? {
        return if (map.isNullOrEmpty()) {
            null
        } else {
            try {
                mapper.writeValueAsString(map)
            } catch (e: Exception) {
                throw IllegalArgumentException("Error converting map to JSON", e)
            }
        }
    }

    override fun convertToEntityAttribute(json: String?): MutableMap<String, Any?> {
        return if (json.isNullOrBlank()) {
            mutableMapOf()
        } else {
            try {
                val typeRef = object : TypeReference<MutableMap<String, Any?>>() {}
                mapper.readValue(json, typeRef)
            } catch (e: Exception) {
                throw IllegalArgumentException("Error converting JSON to map", e)
            }
        }
    }
}
