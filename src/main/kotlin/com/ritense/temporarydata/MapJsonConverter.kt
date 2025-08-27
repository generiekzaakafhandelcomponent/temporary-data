/*
 * Copyright 2015-2025. Ritense BV, the Netherlands.
 *
 *  Licensed under EUPL, Version 1.2 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
