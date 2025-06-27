package com.ritense.temporarydata

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.ritense.zakenapi.domain.ZaakResponse
import com.ritense.zakenapi.event.ZaakCreated
import com.ritense.zgw.Rsin
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.URI
import java.time.LocalDate
import java.util.*


class TemporaryDataServiceImplTest {

    private lateinit var repository: TemporaryDataRepository
    private lateinit var service: TemporaryDataServiceImpl

    private val testZaakUUID = UUID.randomUUID()
    private val testZaakId = "ZAAK-123"
    private val testKey = "testKey"
    private val testValue = "testValue"
    private val testZaakUrl = URI.create("https://api.example.com/zaken/api/v1/zaken/${testZaakUUID}")

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
    fun `createOrUpdateTempData should save ZaakTemporaryData with provided data`() {
        // Given
        val tempData = mapOf("key1" to "value1", "key2" to 42)
        val expectedZaakTempData = ZaakTemporaryData(testZaakUUID, testZaakId, tempData.toMutableMap())

        every { repository.save(any<ZaakTemporaryData>()) } returns expectedZaakTempData

        // When
        service.createOrUpdateTempData(testZaakUUID.toString(), testZaakId, tempData)

        // Then
        verify(exactly = 1) {
            repository.save(match<ZaakTemporaryData> {
                it.zaakUUID == testZaakUUID &&
                        it.zaakId == testZaakId &&
                        it.mapData == tempData.toMutableMap()
            })
        }
    }

    @Test
    fun `createOrUpdateTempData should handle empty map`() {
        // Given
        val emptyTempData = emptyMap<String, Any?>()
        val expectedZaakTempData = ZaakTemporaryData(testZaakUUID, testZaakId, mutableMapOf())

        every { repository.save(any<ZaakTemporaryData>()) } returns expectedZaakTempData

        // When
        service.createOrUpdateTempData(testZaakUUID.toString(), testZaakId, emptyTempData)

        // Then
        verify(exactly = 1) {
            repository.save(match<ZaakTemporaryData> {
                it.zaakUUID == testZaakUUID &&
                        it.zaakId == testZaakId &&
                        it.mapData.isEmpty()
            })
        }
    }

    @Test
    fun `createTempData should save ZaakTemporaryData with empty map`() {
        // Given
        val expectedZaakTempData = ZaakTemporaryData(testZaakUUID, testZaakId, mutableMapOf())

        every { repository.save(any<ZaakTemporaryData>()) } returns expectedZaakTempData

        // When
        service.createTempData(testZaakUUID.toString(), testZaakId)

        // Then
        verify(exactly = 1) {
            repository.save(match<ZaakTemporaryData> {
                it.zaakUUID == testZaakUUID &&
                        it.zaakId == testZaakId &&
                        it.mapData.isEmpty()
            })
        }
    }

    @Test
    fun `storeTempData by UUID should update existing data and save`() {
        // Given
        val existingData = ZaakTemporaryData(testZaakUUID, testZaakId, mutableMapOf("existing" to "data"))
        val updatedData = existingData.copy()
        updatedData.mapData[testKey] = testValue

        every { repository.findByZaakUUID(testZaakUUID) } returns Optional.of(existingData)
        every { repository.save(any<ZaakTemporaryData>()) } returns updatedData

        // When
        service.storeTempData(testZaakUUID, testKey, testValue)

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
            service.storeTempData(testZaakUUID, testKey, testValue)
        }

        verify(exactly = 1) { repository.findByZaakUUID(testZaakUUID) }
        verify(exactly = 0) { repository.save(any<ZaakTemporaryData>()) }
    }

    @Test
    fun `storeTempData by zaakId should update existing data and save`() {
        // Given
        val existingData = ZaakTemporaryData(testZaakUUID, testZaakId, mutableMapOf("existing" to "data"))
        val updatedData = existingData.copy()
        updatedData.mapData[testKey] = testValue

        every { repository.findByZaakId(testZaakId) } returns Optional.of(existingData)
        every { repository.save(any<ZaakTemporaryData>()) } returns updatedData

        // When
        service.storeTempData(testZaakId, testKey, testValue)

        // Then
        verify(exactly = 1) { repository.findByZaakId(testZaakId) }
        verify(exactly = 1) {
            repository.save(match<ZaakTemporaryData> {
                it.mapData[testKey] == testValue &&
                        it.mapData["existing"] == "data"
            })
        }
        assertEquals(testValue, existingData.mapData[testKey])
    }

    @Test
    fun `storeTempData by zaakId should throw exception when data not found`() {
        // Given
        every { repository.findByZaakId(testZaakId) } returns Optional.empty()

        // When & Then
        assertThrows<NoSuchElementException> {
            service.storeTempData(testZaakId, testKey, testValue)
        }

        verify(exactly = 1) { repository.findByZaakId(testZaakId) }
        verify(exactly = 0) { repository.save(any<ZaakTemporaryData>()) }
    }

    @Test
    fun `storeTempData should handle null values`() {
        // Given
        val existingData = ZaakTemporaryData(testZaakUUID, testZaakId, mutableMapOf())

        every { repository.findByZaakUUID(testZaakUUID) } returns Optional.of(existingData)
        every { repository.save(any<ZaakTemporaryData>()) } returns existingData

        // When
        service.storeTempData(testZaakUUID, testKey, null)

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
        val existingData = ZaakTemporaryData(testZaakUUID, testZaakId, mutableMapOf(testKey to testValue))

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
        val existingData = ZaakTemporaryData(testZaakUUID, testZaakId, mutableMapOf())

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
    fun `getTempData by zaakId should return value for existing key`() {
        // Given
        val existingData = ZaakTemporaryData(testZaakUUID, testZaakId, mutableMapOf(testKey to testValue))

        every { repository.findByZaakId(testZaakId) } returns Optional.of(existingData)

        // When
        val result = service.getTempData(testZaakId, testKey)

        // Then
        assertEquals(testValue, result)
        verify(exactly = 1) { repository.findByZaakId(testZaakId) }
    }

    @Test
    fun `getTempData by zaakId should return null for non-existing key`() {
        // Given
        val existingData = ZaakTemporaryData(testZaakUUID, testZaakId, mutableMapOf())

        every { repository.findByZaakId(testZaakId) } returns Optional.of(existingData)

        // When
        val result = service.getTempData(testZaakId, "nonExistingKey")

        // Then
        assertNull(result)
        verify(exactly = 1) { repository.findByZaakId(testZaakId) }
    }

    @Test
    fun `getTempData by zaakId should throw exception when data not found`() {
        // Given
        every { repository.findByZaakId(testZaakId) } returns Optional.empty()

        // When & Then
        assertThrows<NoSuchElementException> {
            service.getTempData(testZaakId, testKey)
        }

        verify(exactly = 1) { repository.findByZaakId(testZaakId) }
    }

    @Test
    fun `getTempData should handle null values correctly`() {
        // Given
        val existingData = ZaakTemporaryData(testZaakUUID, testZaakId, mutableMapOf(testKey to null))

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
        val existingData = ZaakTemporaryData(testZaakUUID, testZaakId, complexData)

        every { repository.findByZaakUUID(testZaakUUID) } returns Optional.of(existingData)

        // When & Then
        assertEquals("test", service.getTempData(testZaakUUID, "stringValue"))
        assertEquals(42, service.getTempData(testZaakUUID, "intValue"))
        assertEquals(true, service.getTempData(testZaakUUID, "booleanValue"))
        assertEquals(listOf(1, 2, 3), service.getTempData(testZaakUUID, "listValue"))
        assertEquals(mapOf("nested" to "value"), service.getTempData(testZaakUUID, "mapValue"))
    }

    @Test
    fun `createTempDataMap should create and save temporary data for valid zaak`() {
        // Given
        val zaakResponse = createValidZaakResponse()
        val event = ZaakCreated(zaakResponse.url.toString(), mapper.valueToTree(zaakResponse))

        every { repository.save(any<ZaakTemporaryData>()) } returns mockk()

        // When
        service.createTempDataMap(event)

        // Then
        verify(exactly = 1) {
            repository.save(match<ZaakTemporaryData> {
                it.zaakUUID == testZaakUUID &&
                        it.zaakId == testZaakId &&
                        it.mapData.isEmpty()
            })
        }
    }

    private fun createValidZaakResponse(): ZaakResponse {
        return ZaakResponse(
            url = testZaakUrl,
            uuid = testZaakUUID,
            identificatie = testZaakId,
            bronorganisatie = Rsin("002564440"),
            zaaktype = URI.create("https://api.example.com/zaaktypen/1"),
            verantwoordelijkeOrganisatie = Rsin("002564440"),
            startdatum = LocalDate.now()
        )
    }
}
