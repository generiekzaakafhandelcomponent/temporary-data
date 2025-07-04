package com.ritense.temporarydata

import com.ritense.document.domain.Document
import com.ritense.processdocument.domain.impl.CamundaProcessInstanceId
import com.ritense.processdocument.service.ProcessDocumentService
import com.ritense.zakenapi.domain.ZaakInstanceLink
import com.ritense.zakenapi.domain.ZaakInstanceLinkId

import com.ritense.zakenapi.link.ZaakInstanceLinkService
import io.mockk.*
import org.camunda.bpm.engine.delegate.VariableScope
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.URI
import java.util.*

class TemporaryDataValueResolverFactoryTest {

    private lateinit var temporaryDataService: TemporaryDataService
    private lateinit var processDocumentService: ProcessDocumentService
    private lateinit var zaakInstanceLinkService: ZaakInstanceLinkService
    private lateinit var variableScope: VariableScope
    private lateinit var factory: TemporaryDataValueResolverFactory

    private val testDocumentId = "550e8400-e29b-41d4-a716-446655440000"
    private val testDocumentUUID = UUID.fromString(testDocumentId)
    private val documentIdentifier = mockk<Document.Id>()
    private val testProcessInstanceId = UUID.randomUUID().toString()
    private val testZaakUUID = UUID.randomUUID()
    private val testZaakUrl = URI.create("https://api.example.com/zaken/api/v1/zaken/${testZaakUUID}")
    private val testRequestedValue = "testKey"
    private val testValue = "testValue"
    private val testLinkId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        every{documentIdentifier.toString()} returns testDocumentId

        temporaryDataService = mockk()
        processDocumentService = mockk()
        zaakInstanceLinkService = mockk()
        variableScope = mockk()

        factory = TemporaryDataValueResolverFactory(
            temporaryDataService,
            processDocumentService,
            zaakInstanceLinkService,
        )
    }

    @Test
    fun `supportedPrefix should return correct prefix`() {
        // When
        val result = factory.supportedPrefix()

        // Then
        assertEquals("tzd", result)
    }

    @Test
    fun `createResolver with documentId should return function that resolves temp data`() {
        // Given
        val zaakInstanceLink = createMockZaakInstanceLink()

        every { zaakInstanceLinkService.getByDocumentId(testDocumentUUID) } returns zaakInstanceLink
        every { zaakInstanceLink.zaakInstanceId } returns testZaakUUID
        every { temporaryDataService.getTempData(testZaakUUID, testRequestedValue) } returns testValue

        // When
        val resolver = factory.createResolver(testDocumentId)
        val result = resolver.apply(testRequestedValue)

        // Then
        assertEquals(testValue, result)
        verify(exactly = 1) { zaakInstanceLinkService.getByDocumentId(testDocumentUUID) }
        verify(exactly = 1) { temporaryDataService.getTempData(testZaakUUID, testRequestedValue) }
    }


    @Test
    fun `createResolver with documentId should handle invalid UUID format`() {
        // Given
        val invalidDocumentId = "invalid-uuid"

        // When & Then
        val resolver = factory.createResolver(invalidDocumentId)
        assertThrows<IllegalArgumentException> {
            resolver.apply(testRequestedValue)
        }
    }

    @Test
    fun `createResolver with processInstanceId should return function that resolves temp data`() {
        // Given
        val zaakInstanceLink = createMockZaakInstanceLink()

        every { processDocumentService.getDocumentId(
            CamundaProcessInstanceId(testProcessInstanceId),
            variableScope
        ) } returns documentIdentifier
        every { zaakInstanceLinkService.getByDocumentId(testDocumentUUID) } returns zaakInstanceLink
        every { zaakInstanceLink.zaakInstanceId } returns testZaakUUID
        every { temporaryDataService.getTempData(testZaakUUID, testRequestedValue) } returns testValue

        // When
        val resolver = factory.createResolver(testProcessInstanceId, variableScope)
        val result = resolver.apply(testRequestedValue)

        // Then
        assertEquals(testValue, result)
        verify(exactly = 1) {
            processDocumentService.getDocumentId(
                CamundaProcessInstanceId(testProcessInstanceId),
                variableScope
            )
        }
        verify(exactly = 1) { zaakInstanceLinkService.getByDocumentId(testDocumentUUID) }
        verify(exactly = 1) { temporaryDataService.getTempData(testZaakUUID, testRequestedValue) }
    }

    @Test
    fun `handleValues should create or update temp data successfully`() {
        // Given
        val values = mapOf("key1" to "value1", "key2" to 42)
        val zaakInstanceLink = createMockZaakInstanceLink()

        every { processDocumentService.getDocumentId(
            CamundaProcessInstanceId(testProcessInstanceId),
            variableScope
        ) } returns documentIdentifier
        every { zaakInstanceLinkService.getByDocumentId(testDocumentUUID) } returns zaakInstanceLink
        every { zaakInstanceLink.zaakInstanceId } returns testZaakUUID

        every { temporaryDataService.createOrUpdateTempData(testZaakUUID.toString(), values) } just Runs


        // When
        factory.handleValues(testProcessInstanceId, variableScope, values)

        // Then
        verify(exactly = 1) {
            processDocumentService.getDocumentId(
                CamundaProcessInstanceId(testProcessInstanceId),
                variableScope
            )
        }
        verify(exactly = 1) { zaakInstanceLinkService.getByDocumentId(testDocumentUUID) }
        verify(exactly = 1) { temporaryDataService.createOrUpdateTempData(testZaakUUID.toString(), values) }
    }

    @Test
    fun `handleValues should handle empty values map`() {
        // Given
        val emptyValues = emptyMap<String, Any?>()
        val zaakInstanceLink = createMockZaakInstanceLink()

        every { processDocumentService.getDocumentId(
            CamundaProcessInstanceId(testProcessInstanceId),
            variableScope
        ) } returns documentIdentifier
        every { zaakInstanceLinkService.getByDocumentId(testDocumentUUID) } returns zaakInstanceLink
        every { zaakInstanceLink.zaakInstanceId } returns testZaakUUID
        every { temporaryDataService.createOrUpdateTempData(testZaakUUID.toString(), emptyValues) } just Runs

        // When
        factory.handleValues(testProcessInstanceId, variableScope, emptyValues)

        // Then
        verify(exactly = 1) { temporaryDataService.createOrUpdateTempData(testZaakUUID.toString(), emptyValues) }
    }

    @Test
    fun `should handle values successfully with valid document ID and values`() {
        // Given
        val documentId = UUID.randomUUID()
        val zaakInstanceId = UUID.randomUUID()
        val values = mapOf("key1" to "value1", "key2" to 42, "key3" to true)
        val zaakInstanceLink = createZaakInstanceLink(documentId, zaakInstanceId)

        every { zaakInstanceLinkService.getByDocumentId(documentId) } returns zaakInstanceLink
        every { temporaryDataService.createOrUpdateTempData(zaakInstanceId.toString(), values) } just Runs

        // When
        factory.handleValues(documentId, values)

        // Then
        verify { zaakInstanceLinkService.getByDocumentId(documentId) }
        verify { temporaryDataService.createOrUpdateTempData(zaakInstanceId.toString(), values) }
    }


    @Test
    fun `resolver functions should be reusable`() {
        // Given
        val zaakInstanceLink = createMockZaakInstanceLink()

        every { zaakInstanceLinkService.getByDocumentId(testDocumentUUID) } returns zaakInstanceLink
        every { zaakInstanceLink.zaakInstanceId } returns testZaakUUID
        every { temporaryDataService.getTempData(testZaakUUID, "key1") } returns "value1"
        every { temporaryDataService.getTempData(testZaakUUID, "key2") } returns "value2"

        // When
        val resolver = factory.createResolver(testDocumentId)
        val result1 = resolver.apply("key1")
        val result2 = resolver.apply("key2")

        // Then
        assertEquals("value1", result1)
        assertEquals("value2", result2)
        verify(exactly = 2) { zaakInstanceLinkService.getByDocumentId(testDocumentUUID) }
        verify(exactly = 1) { temporaryDataService.getTempData(testZaakUUID, "key1") }
        verify(exactly = 1) { temporaryDataService.getTempData(testZaakUUID, "key2") }
    }

    private fun createMockZaakInstanceLink(): ZaakInstanceLink {
        return mockk<ZaakInstanceLink> {
            every { zaakInstanceLinkId } returns ZaakInstanceLinkId(testLinkId)
            every { zaakInstanceUrl } returns testZaakUrl
        }
    }

    private fun createZaakInstanceLink(documentId: UUID, zaakInstanceId: UUID): ZaakInstanceLink {
        return mockk<ZaakInstanceLink> {
            every { this@mockk.documentId } returns documentId
            every { this@mockk.zaakInstanceId } returns zaakInstanceId
        }
    }
}
