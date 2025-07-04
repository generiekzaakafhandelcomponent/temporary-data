package com.ritense.temporarydata

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule

import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

import java.util.*


class TemporaryDataServiceImplTest {

    private lateinit var repository: TemporaryDataRepository
    private lateinit var service: TemporaryDataServiceImpl

    private val testZaakUUID = UUID.randomUUID()
    private val testKey = "testKey"
    private val testValue = "testValue"

    private lateinit var mapper: ObjectMapper




    @BeforeEach
    fun setUp() {
        mapper = ObjectMapper()
        mapper.registerModule(JavaTimeModule())
        mapper.registerModule(KotlinModule())
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        repository = mockk()
        service = TemporaryDataServiceImpl(repository, mapper)
    }

    @Test
    fun `should create new temp data when zaak does not exist`() {
        // Given
        val zaakUUID = "550e8400-e29b-41d4-a716-446655440000"
        val tempData = mapOf("key1" to "value1", "key2" to 42)
        val expectedUUID = UUID.fromString(zaakUUID)

        every { repository.existsByZaakUUID(expectedUUID) } returns false
        every { repository.save(any<ZaakTemporaryData>()) } returns mockk()

        // When
        service.createOrUpdateTempData(zaakUUID, tempData)

        // Then
        verify { repository.existsByZaakUUID(expectedUUID) }
        verify {
            repository.save(match<ZaakTemporaryData> {
                it.zaakUUID == expectedUUID && it.mapData == tempData.toMutableMap()
            })
        }
        verify(exactly = 0) { repository.findByZaakUUID(any()) }
    }

    @Test
    fun `should update existing temp data when zaak exists`() {
        // Given
        val zaakUUID = "550e8400-e29b-41d4-a716-446655440000"
        val expectedUUID = UUID.fromString(zaakUUID)
        val existingData = mutableMapOf("existingKey" to "existingValue") as MutableMap<String, Any?>
        val tempData = mapOf("key1" to "value1", "key2" to 42)
        val existingZaakData = ZaakTemporaryData(expectedUUID, existingData)

        every { repository.existsByZaakUUID(expectedUUID) } returns true
        every { repository.findByZaakUUID(expectedUUID) } returns Optional.of(existingZaakData)
        every { repository.save(any<ZaakTemporaryData>()) } returns mockk()

        // When
        service.createOrUpdateTempData(zaakUUID, tempData)

        // Then
        verify { repository.existsByZaakUUID(expectedUUID) }
        verify { repository.findByZaakUUID(expectedUUID) }
        verify { repository.save(existingZaakData) }

        // Verify that the data was merged correctly
        val expectedMergedData = mutableMapOf(
            "existingKey" to "existingValue",
            "key1" to "value1",
            "key2" to 42
        )
        assertEquals(expectedMergedData, existingZaakData.mapData)
    }

    @Test
    fun `should overwrite existing keys when updating temp data`() {
        // Given
        val zaakUUID = "550e8400-e29b-41d4-a716-446655440000"
        val expectedUUID = UUID.fromString(zaakUUID)
        val existingData = mutableMapOf("key1" to "oldValue", "key2" to "keepThis") as MutableMap<String, Any?>
        val tempData = mapOf("key1" to "newValue", "key3" to "addThis")
        val existingZaakData = ZaakTemporaryData(expectedUUID, existingData)

        every { repository.existsByZaakUUID(expectedUUID) } returns true
        every { repository.findByZaakUUID(expectedUUID) } returns Optional.of(existingZaakData)
        every { repository.save(any<ZaakTemporaryData>()) } returns mockk()

        // When
        service.createOrUpdateTempData(zaakUUID, tempData)

        // Then
        val expectedMergedData = mutableMapOf(
            "key1" to "newValue",
            "key2" to "keepThis",
            "key3" to "addThis"
        )
        assertEquals(expectedMergedData, existingZaakData.mapData)
        verify { repository.save(existingZaakData) }
    }

    @Test
    fun `should handle empty temp data map`() {
        // Given
        val zaakUUID = "550e8400-e29b-41d4-a716-446655440000"
        val expectedUUID = UUID.fromString(zaakUUID)
        val tempData = emptyMap<String, Any?>()

        every { repository.existsByZaakUUID(expectedUUID) } returns false
        every { repository.save(any<ZaakTemporaryData>()) } returns mockk()

        // When
        service.createOrUpdateTempData(zaakUUID, tempData)

        // Then
        verify {
            repository.save(match<ZaakTemporaryData> {
                it.zaakUUID == expectedUUID && it.mapData.isEmpty()
            })
        }
    }

    @Test
    fun `should handle null values in temp data`() {
        // Given
        val zaakUUID = "550e8400-e29b-41d4-a716-446655440000"
        val expectedUUID = UUID.fromString(zaakUUID)
        val tempData = mapOf("key1" to "value1", "key2" to null, "key3" to 42)

        every { repository.existsByZaakUUID(expectedUUID) } returns false
        every { repository.save(any<ZaakTemporaryData>()) } returns mockk()

        // When
        service.createOrUpdateTempData(zaakUUID, tempData)

        // Then
        verify {
            repository.save(match<ZaakTemporaryData> {
                it.zaakUUID == expectedUUID &&
                        it.mapData == tempData.toMutableMap() &&
                        it.mapData["key2"] == null
            })
        }
    }

    @Test
    fun `createTempData should save ZaakTemporaryData with empty map`() {
        // Given
        val expectedZaakTempData = ZaakTemporaryData(testZaakUUID, mutableMapOf())

        every { repository.save(any<ZaakTemporaryData>()) } returns expectedZaakTempData

        // When
        service.createTempData(testZaakUUID.toString())

        // Then
        verify(exactly = 1) {
            repository.save(match<ZaakTemporaryData> {
                it.zaakUUID == testZaakUUID &&
                        it.mapData.isEmpty()
            })
        }
    }

    @Test
    fun `storeTempData by UUID should update existing data and save`() {
        // Given
        val existingData = ZaakTemporaryData(testZaakUUID, mutableMapOf("existing" to "data"))
        val updatedData = existingData.copy()
        updatedData.mapData[testKey] = testValue

        every { repository.findByZaakUUID(testZaakUUID) } returns Optional.of(existingData)
        every { repository.save(any<ZaakTemporaryData>()) } returns updatedData

        // When
        service.storeTempData(testZaakUUID.toString(), testKey, testValue)

        // Then
        verify(exactly = 1) { repository.findByZaakUUID(testZaakUUID) }
        verify(exactly = 1) {
            repository.save(match<ZaakTemporaryData> {
                it.mapData[testKey] == testValue &&
                        it.mapData["existing"] == "data"
            })
        }
        assertEquals(testValue, existingData.mapData[testKey])
    }

    @Test
    fun `storeTempData by UUID should throw exception when data not found`() {
        // Given
        every { repository.findByZaakUUID(testZaakUUID) } returns Optional.empty()

        // When & Then
        assertThrows<NoSuchElementException> {
            service.storeTempData(testZaakUUID.toString(), testKey, testValue)
        }

        verify(exactly = 1) { repository.findByZaakUUID(testZaakUUID) }
        verify(exactly = 0) { repository.save(any<ZaakTemporaryData>()) }
    }

    @Test
    fun `storeTempData should handle null values`() {
        // Given
        val existingData = ZaakTemporaryData(testZaakUUID, mutableMapOf())

        every { repository.findByZaakUUID(testZaakUUID) } returns Optional.of(existingData)
        every { repository.save(any<ZaakTemporaryData>()) } returns existingData

        // When
        service.storeTempData(testZaakUUID.toString(), testKey, null)

        // Then
        verify(exactly = 1) { repository.findByZaakUUID(testZaakUUID) }
        verify(exactly = 1) {
            repository.save(match<ZaakTemporaryData> {
                it.mapData[testKey] == null
            })
        }
        assertNull(existingData.mapData[testKey])
    }

    @Test
    fun `getTempData by UUID should return value for existing key`() {
        // Given
        val existingData = ZaakTemporaryData(testZaakUUID, mutableMapOf(testKey to testValue))

        every { repository.findByZaakUUID(testZaakUUID) } returns Optional.of(existingData)

        // When
        val result = service.getTempData(testZaakUUID, testKey)

        // Then
        assertEquals(testValue, result)
        verify(exactly = 1) { repository.findByZaakUUID(testZaakUUID) }
    }

    @Test
    fun `getTempData by UUID should return null for non-existing key`() {
        // Given
        val existingData = ZaakTemporaryData(testZaakUUID, mutableMapOf())

        every { repository.findByZaakUUID(testZaakUUID) } returns Optional.of(existingData)

        // When
        val result = service.getTempData(testZaakUUID, "nonExistingKey")

        // Then
        assertNull(result)
        verify(exactly = 1) { repository.findByZaakUUID(testZaakUUID) }
    }

    @Test
    fun `getTempData by UUID should throw exception when data not found`() {
        // Given
        every { repository.findByZaakUUID(testZaakUUID) } returns Optional.empty()

        // When & Then
        assertThrows<NoSuchElementException> {
            service.getTempData(testZaakUUID, testKey)
        }

        verify(exactly = 1) { repository.findByZaakUUID(testZaakUUID) }
    }



    @Test
    fun `getTempData should handle null values correctly`() {
        // Given
        val existingData = ZaakTemporaryData(testZaakUUID, mutableMapOf(testKey to null))

        every { repository.findByZaakUUID(testZaakUUID) } returns Optional.of(existingData)

        // When
        val result = service.getTempData(testZaakUUID, testKey)

        // Then
        assertNull(result)
        verify(exactly = 1) { repository.findByZaakUUID(testZaakUUID) }
    }

    @Test
    fun `removeZaakTempData should call repository deleteByZaakUUID`() {
        // Given
        every { repository.deleteByZaakUUID(testZaakUUID) } just Runs

        // When
        service.removeZaakTempData(testZaakUUID)

        // Then
        verify(exactly = 1) { repository.deleteByZaakUUID(testZaakUUID) }
    }

    @Test
    fun `getTempData should handle different data types`() {
        // Given
        val complexData = mutableMapOf<String, Any?>(
            "stringValue" to "test",
            "intValue" to 42,
            "booleanValue" to true,
            "listValue" to listOf(1, 2, 3),
            "mapValue" to mapOf("nested" to "value")
        )
        val existingData = ZaakTemporaryData(testZaakUUID, complexData)

        every { repository.findByZaakUUID(testZaakUUID) } returns Optional.of(existingData)

        // When & Then
        assertEquals("test", service.getTempData(testZaakUUID, "stringValue"))
        assertEquals(42, service.getTempData(testZaakUUID, "intValue"))
        assertEquals(true, service.getTempData(testZaakUUID, "booleanValue"))
        assertEquals(listOf(1, 2, 3), service.getTempData(testZaakUUID, "listValue"))
        assertEquals(mapOf("nested" to "value"), service.getTempData(testZaakUUID, "mapValue"))
    }

}
