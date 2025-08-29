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

import org.junit.jupiter.api.Assertions.*
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MapJsonConverterTest {

    private lateinit var objectMapper: ObjectMapper
    private lateinit var converter: MapJsonConverter

    @BeforeEach
    fun setUp() {
        objectMapper = mockk()
        converter = MapJsonConverter(objectMapper)
    }

    @Test
    fun `convertToDatabaseColumn should convert map to JSON string`() {
        // Given
        val inputMap = mapOf("key1" to "value1", "key2" to 42, "key3" to true)
        val expectedJson = """{"key1":"value1","key2":42,"key3":true}"""

        every { objectMapper.writeValueAsString(inputMap) } returns expectedJson

        // When
        val result = converter.convertToDatabaseColumn(inputMap)

        // Then
        assertEquals(expectedJson, result)
        verify(exactly = 1) { objectMapper.writeValueAsString(inputMap) }
    }

    @Test
    fun `convertToDatabaseColumn should handle empty map`() {
        // Given
        val emptyMap = emptyMap<String, Any>()
        val expectedJson = "{}"

        every { objectMapper.writeValueAsString(emptyMap) } returns expectedJson

        // When
        val result = converter.convertToDatabaseColumn(emptyMap)

        // Then
        assertEquals(expectedJson, result)
        verify(exactly = 1) { objectMapper.writeValueAsString(emptyMap) }
    }


    @Test
    fun `convertToDatabaseColumn should handle complex nested objects`() {
        // Given
        val complexMap = mapOf(
            "simpleString" to "value",
            "number" to 123,
            "nestedMap" to mapOf("nested" to "value"),
            "list" to listOf(1, 2, 3)
        )
        val expectedJson = """{"simpleString":"value","number":123,"nestedMap":{"nested":"value"},"list":[1,2,3]}"""

        every { objectMapper.writeValueAsString(complexMap) } returns expectedJson

        // When
        val result = converter.convertToDatabaseColumn(complexMap)

        // Then
        assertEquals(expectedJson, result)
        verify(exactly = 1) { objectMapper.writeValueAsString(complexMap) }
    }


    @Test
    fun `convertToEntityAttribute should convert JSON string to mutable map`() {
        // Given
        val jsonString = """{"key1":"value1","key2":42,"key3":true}"""
        val expectedMap = mutableMapOf<String, Any>("key1" to "value1", "key2" to 42, "key3" to true)

        every { objectMapper.readValue(jsonString, any<com.fasterxml.jackson.core.type.TypeReference<MutableMap<String, Any>>>()) } returns expectedMap

        // When
        val result = converter.convertToEntityAttribute(jsonString)

        // Then
        assertEquals(expectedMap, result)
        verify(exactly = 1) { objectMapper.readValue(jsonString, any<com.fasterxml.jackson.core.type.TypeReference<MutableMap<String, Any>>>()) }
    }

    @Test
    fun `convertToEntityAttribute should handle empty JSON object`() {
        // Given
        val jsonString = "{}"
        val expectedMap = mutableMapOf<String, Any>()

        every { objectMapper.readValue(jsonString, any<com.fasterxml.jackson.core.type.TypeReference<MutableMap<String, Any>>>()) } returns expectedMap

        // When
        val result = converter.convertToEntityAttribute(jsonString)

        // Then
        assertEquals(expectedMap, result)
        assertTrue(result.isEmpty())
        verify(exactly = 1) { objectMapper.readValue(jsonString, any<com.fasterxml.jackson.core.type.TypeReference<MutableMap<String, Any>>>()) }
    }

    @Test
    fun `convertToEntityAttribute should handle complex nested JSON`() {
        // Given
        val jsonString = """{"simpleString":"value","number":123,"nestedMap":{"nested":"value"},"list":[1,2,3]}"""
        val expectedMap = mutableMapOf<String, Any>(
            "simpleString" to "value",
            "number" to 123,
            "nestedMap" to mapOf("nested" to "value"),
            "list" to listOf(1, 2, 3)
        )

        every { objectMapper.readValue(jsonString, any<com.fasterxml.jackson.core.type.TypeReference<MutableMap<String, Any>>>()) } returns expectedMap

        // When
        val result = converter.convertToEntityAttribute(jsonString)

        // Then
        assertEquals(expectedMap, result)
        assertEquals("value", result["simpleString"])
        assertEquals(123, result["number"])
        assertEquals(mapOf("nested" to "value"), result["nestedMap"])
        assertEquals(listOf(1, 2, 3), result["list"])
        verify(exactly = 1) { objectMapper.readValue(jsonString, any<com.fasterxml.jackson.core.type.TypeReference<MutableMap<String, Any>>>()) }
    }

    @Test
    fun `convertToEntityAttribute should return empty map for empty string`() {
        // When
        val result = converter.convertToEntityAttribute("")

        // Then
        assertTrue(result.isEmpty())
        verify(exactly = 0) { objectMapper.readValue(any<String>(), any<com.fasterxml.jackson.core.type.TypeReference<MutableMap<String, Any>>>()) }
    }

    @Test
    fun `convertToEntityAttribute should return empty map for blank string`() {
        // When
        val result = converter.convertToEntityAttribute("   ")

        // Then
        assertTrue(result.isEmpty())
        verify(exactly = 0) { objectMapper.readValue(any<String>(), any<com.fasterxml.jackson.core.type.TypeReference<MutableMap<String, Any>>>()) }
    }


    @Test
    fun `convertToEntityAttribute should return mutable map that can be modified`() {
        // Given
        val jsonString = """{"key1":"value1"}"""
        val initialMap = mutableMapOf<String, Any>("key1" to "value1")

        every { objectMapper.readValue(jsonString, any<com.fasterxml.jackson.core.type.TypeReference<MutableMap<String, Any>>>()) } returns initialMap

        // When
        val result = converter.convertToEntityAttribute(jsonString)

        // Then
        // Verify the returned map is mutable
        result["newKey"] = "newValue"
        assertEquals("newValue", result["newKey"])
        assertEquals(2, result.size)
    }

    @Test
    fun `round trip conversion should preserve data integrity`() {
        // Given
        val originalMap = mapOf(
            "string" to "test",
            "number" to 42,
            "boolean" to true
        )
        val jsonString = """{"string":"test","number":42,"boolean":true}"""
        val deserializedMap = mutableMapOf<String, Any>(
            "string" to "test",
            "number" to 42,
            "boolean" to true
        )

        every { objectMapper.writeValueAsString(originalMap) } returns jsonString
        every { objectMapper.readValue(jsonString, any<com.fasterxml.jackson.core.type.TypeReference<MutableMap<String, Any>>>()) } returns deserializedMap

        // When
        val serialized = converter.convertToDatabaseColumn(originalMap)
        val deserialized = converter.convertToEntityAttribute(serialized)

        // Then
        assertEquals(jsonString, serialized)
        assertEquals(deserializedMap, deserialized)
        assertEquals(originalMap["string"], deserialized["string"])
        assertEquals(originalMap["number"], deserialized["number"])
        assertEquals(originalMap["boolean"], deserialized["boolean"])
    }
}
