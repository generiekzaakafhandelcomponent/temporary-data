package com.ritense.temporarydata

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.jayway.jsonpath.JsonPath
import com.ritense.authorization.AuthorizationContext.Companion.runWithoutAuthorization
import com.ritense.document.domain.impl.request.NewDocumentRequest
import com.ritense.document.service.impl.JsonSchemaDocumentService
import com.ritense.form.repository.FormDefinitionRepository
import com.ritense.form.service.FormSubmissionService
import com.ritense.form.service.PrefillFormService
import com.ritense.processdocument.domain.impl.request.NewDocumentAndStartProcessRequest
import com.ritense.processdocument.domain.impl.request.StartProcessForDocumentRequest
import com.ritense.processdocument.service.ProcessDocumentService
import com.ritense.processlink.service.ProcessLinkService
import com.ritense.zakenapi.domain.ZaakResponse
import com.ritense.zakenapi.event.ZaakCreated
import com.ritense.zakenapi.link.ZaakInstanceLinkService
import com.ritense.zgw.Rsin
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URI
import java.util.UUID
import kotlin.test.Test
import org.assertj.core.api.Assertions.assertThat
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.TaskService
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDate
import java.util.LinkedHashMap

class TemporaryDataValueResolverFactoryIT @Autowired constructor(
    private val documentService: JsonSchemaDocumentService,
    private val formDefinitionRepository: FormDefinitionRepository,
    private val prefillFormService: PrefillFormService,
    private val objectMapper: ObjectMapper,
    private val zaakInstanceLinkService: ZaakInstanceLinkService,
    private val temporaryDataService: TemporaryDataService,
    private val processDocumentService: ProcessDocumentService,
    private val formSubmissionService: FormSubmissionService,
    private val processLinkService: ProcessLinkService,
    private val taskService: TaskService,
    private val eventPublisher: ApplicationEventPublisher
    ): BaseIntegrationTest() {

    @Test
    fun `should prefill form with data from the Temporary Data repo`() {
        runWithoutAuthorization {
            val documentId = documentService.createDocument(
                NewDocumentRequest("profile", objectMapper.createObjectNode())
            ).resultingDocument().get().id.id

            val zaakInstanceId = UUID.randomUUID()
            zaakInstanceLinkService.createZaakInstanceLink(URI.create(""), zaakInstanceId, documentId, URI.create(""))

            temporaryDataService.createOrUpdateTempData(
                zaakInstanceId.toString(),
                mapOf("relatieNummer" to "012345", "factuurNummer" to "98765")
            )

            val formDefinition = formDefinitionRepository.findByName("form-with-temporarydata-fields").get()
            val prefilledFormDefinition = prefillFormService.getPrefilledFormDefinition(
                formDefinition.id!!,
                documentId
            )
            assertThat(
                JsonPath.read<List<String>>(
                    prefilledFormDefinition.asJson().toString(),
                    "$.components[?(@.properties.sourceKey=='tzd:relatieNummer')].defaultValue"
                ).toString()
            ).isEqualTo("""["012345"]""")
        }
    }

    @Test
    fun `should store temporary data from form`() {
        runWithoutAuthorization {
            // Given
            val definitionProcessLink = processLinkService.getProcessLinksByProcessDefinitionKey("TestProcess")[0]

            val newDocumentAndStartProcess = processDocumentService.newDocumentAndStartProcess(
                NewDocumentAndStartProcessRequest(
                    PROCESS_DEFINITION_KEY,
                    NewDocumentRequest(DOCUMENT_DEFINITION_KEY, objectMapper.createObjectNode())
                )
            )
            val documentId = newDocumentAndStartProcess.resultingDocument().orElseThrow().id().id
            val processInstanceId = newDocumentAndStartProcess.resultingProcessInstanceId().orElseThrow().toString()

            val zaakInstanceId = UUID.randomUUID()
            zaakInstanceLinkService.createZaakInstanceLink(URI.create(""), zaakInstanceId, documentId, URI.create(""))

            val task = taskService.createTaskQuery().processInstanceId(processInstanceId).active().singleResult()

            val formData = objectMapper.createObjectNode()
                .put("relatieNummer", "123456")
                .put("factuurNummer", "87654")

            // When
            formSubmissionService.handleSubmission(
                definitionProcessLink.id,
                formData,
                DOCUMENT_DEFINITION_KEY,
                documentId.toString(),
                task.id
            )

            // Then
            assertThat(temporaryDataService.getTempData(zaakInstanceId, "relatieNummer")).isEqualTo("123456")
            assertThat(temporaryDataService.getTempData(zaakInstanceId, "factuurNummer")).isEqualTo("87654")
        }
    }

    @Test
    fun `should prefill form with data using nested proprties from the Temporary Data repo`() {
        runWithoutAuthorization {
            val documentId = documentService.createDocument(
                NewDocumentRequest("profile", objectMapper.createObjectNode())
            ).resultingDocument().get().id.id

            val zaakInstanceId = UUID.randomUUID()
            zaakInstanceLinkService.createZaakInstanceLink(URI.create(""), zaakInstanceId, documentId, URI.create(""))

            val json = """
                 {
                    "titel":"Erpacht is te hoog",
                    "omschrijving":"Mijn erfpacht is in de nieuwe situatie veel te hoog"
                 }
              """

            val klacht  = objectMapper.readTree(json)

            temporaryDataService.createOrUpdateTempData(
                zaakInstanceId.toString(),
                mapOf("relatieNummer" to "012345", "factuurNummer" to "98765", "klacht" to klacht)
            )

            val formDefinition = formDefinitionRepository.findByName("form-with-temporarydata-fields").get()
            val prefilledFormDefinition = prefillFormService.getPrefilledFormDefinition(
                formDefinition.id!!,
                documentId
            )
            assertThat(
                JsonPath.read<List<String>>(
                    prefilledFormDefinition.asJson().toString(),
                    "$.components[?(@.properties.sourceKey=='tzd:relatieNummer')].defaultValue"
                ).toString()
            ).isEqualTo("""["012345"]""")

            assertThat(
                JsonPath.read<List<String>>(
                    prefilledFormDefinition.asJson().toString(),
                    "$.components[?(@.properties.sourceKey=='tzd:klacht.titel')].defaultValue"
                ).toString()
            ).isEqualTo("""["Erpacht is te hoog"]""")
        }
    }

    @Test
    fun `should store temporary data with nested properties from form`() {
        runWithoutAuthorization {
            // Given
            val definitionProcessLink = processLinkService.getProcessLinksByProcessDefinitionKey("TestProcess")[0]

            val newDocumentAndStartProcess = processDocumentService.newDocumentAndStartProcess(
                NewDocumentAndStartProcessRequest(
                    PROCESS_DEFINITION_KEY,
                    NewDocumentRequest(DOCUMENT_DEFINITION_KEY, objectMapper.createObjectNode())
                )
            )

            val documentId = newDocumentAndStartProcess.resultingDocument().orElseThrow().id().id
            val processInstanceId = newDocumentAndStartProcess.resultingProcessInstanceId().orElseThrow().toString()

            val zaakInstanceId = UUID.randomUUID()
            zaakInstanceLinkService.createZaakInstanceLink(URI.create(""), zaakInstanceId, documentId, URI.create(""))

            val task = taskService.createTaskQuery().processInstanceId(processInstanceId).active().singleResult()

            val formData = objectMapper.createObjectNode()
                .put("relatieNummer", "123456")
                .put("factuurNummer", "87654")
                .put("klachtTitel", "Erpacht is te hoog")
                .put("klachtOmschrijving", "Mijn erfpacht is in de nieuwe situatie veel te hoog")

            // When
            formSubmissionService.handleSubmission(
                definitionProcessLink.id,
                formData,
                DOCUMENT_DEFINITION_KEY,
                documentId.toString(),
                task.id
            )


            val json = """
                 {
                    "titel":"Erpacht is te hoog",
                    "omschrijving":"Mijn erfpacht is in de nieuwe situatie veel te hoog"
                 }
              """

            // Then
            assertThat(temporaryDataService.getTempData(zaakInstanceId, "klacht.titel")).isEqualTo("Erpacht is te hoog")
            val klacht = temporaryDataService.getTempData(zaakInstanceId, "klacht")
            val serializedKLacht = objectMapper.writeValueAsString(klacht)
            JSONAssert.assertEquals(json, serializedKLacht, false)
        }
    }

    @Test
    fun `should update data with form using nested proprties from the Temporary Data repo`() {
        runWithoutAuthorization {
            //prefill with valiues
            val documentId = documentService.createDocument(
                NewDocumentRequest("profile", objectMapper.createObjectNode())
            ).resultingDocument().get().id()

            val zaakInstanceId = UUID.randomUUID()
            zaakInstanceLinkService.createZaakInstanceLink(URI.create(""), zaakInstanceId, documentId.id, URI.create(""))

            val json = """
                 {
                    "titel":"Erpacht is te hoog",
                    "omschrijving":"Mijn erfpacht is in de nieuwe situatie veel te hoog"
                 }
              """

            val klacht = objectMapper.readTree(json)

            temporaryDataService.createOrUpdateTempData(
                zaakInstanceId.toString(),
                mapOf("relatieNummer" to "012345", "factuurNummer" to "98765", "klacht" to klacht)
            )

            val formDefinition = formDefinitionRepository.findByName("form-with-temporarydata-fields").get()
            val prefilledFormDefinition = prefillFormService.getPrefilledFormDefinition(
                formDefinition.id!!,
                documentId.id
            )

            assertThat(
                JsonPath.read<List<String>>(
                    prefilledFormDefinition.asJson().toString(),
                    "$.components[?(@.properties.sourceKey=='tzd:klacht.titel')].defaultValue"
                ).toString()
            ).isEqualTo("""["Erpacht is te hoog"]""")

            // start process with prefilled form and change form
            val definitionProcessLink = processLinkService.getProcessLinksByProcessDefinitionKey("TestProcess")[0]
            val startProcSoc = StartProcessForDocumentRequest(documentId,
                PROCESS_DEFINITION_KEY,
                mapOf("relatieNummer" to "012345", "factuurNummer" to "98765")
            )
            val startResult = processDocumentService.startProcessForDocument(startProcSoc)

            val task = taskService.createTaskQuery().processInstanceId(startResult.processInstanceId().get().toString()).active().singleResult()

            val formData = objectMapper.createObjectNode()
                .put("relatieNummer", "245678")
                .put("factuurNummer", "87654")
                .put("klachtTitel", "Erpacht is te laag")
                .put("klachtOmschrijving", "Jouw erfpacht is in de nieuwe situatie veel te laag")

            // When
            formSubmissionService.handleSubmission(
                definitionProcessLink.id,
                formData,
                DOCUMENT_DEFINITION_KEY,
                documentId.toString(),
                task.id
            )

            //then
            assertThat(temporaryDataService.getTempData(zaakInstanceId, "relatieNummer")).isEqualTo("245678")
            assertThat(temporaryDataService.getTempData(zaakInstanceId, "klacht.titel")).isEqualTo("Erpacht is te laag")

        }
    }

    @Test
    fun `should update data using service method using nested proprties from the Temporary Data repo`() {
        runWithoutAuthorization {
            //prefill with valiues
            val documentId = documentService.createDocument(
                NewDocumentRequest("profile", objectMapper.createObjectNode())
            ).resultingDocument().get().id()

            val zaakInstanceId = UUID.randomUUID()
            zaakInstanceLinkService.createZaakInstanceLink(URI.create(""), zaakInstanceId, documentId.id, URI.create(""))

            val json = """
                 {
                    "titel":"Erpacht is te hoog",
                    "omschrijving":"Mijn erfpacht is in de nieuwe situatie veel te hoog"
                 }
              """

            val klacht = objectMapper.readTree(json)

            temporaryDataService.createOrUpdateTempData(
                zaakInstanceId.toString(),
                mapOf("relatieNummer" to "012345", "factuurNummer" to "98765", "klacht" to klacht)
            )

            val formDefinition = formDefinitionRepository.findByName("form-with-temporarydata-fields").get()
            val prefilledFormDefinition = prefillFormService.getPrefilledFormDefinition(
                formDefinition.id!!,
                documentId.id
            )

            assertThat(
                JsonPath.read<List<String>>(
                    prefilledFormDefinition.asJson().toString(),
                    "$.components[?(@.properties.sourceKey=='tzd:klacht.titel')].defaultValue"
                ).toString()
            ).isEqualTo("""["Erpacht is te hoog"]""")



            // When
            // use service method mimicking service tasks.
            val jsonUpdate = """
                 {
                    "titel":"Erpacht is te laag",
                    "omschrijving":"Jouw erfpacht is in de nieuwe situatie veel te laag"
                 }
              """

            val klachtUpdate = objectMapper.readTree(jsonUpdate)

            temporaryDataService.storeTempData(
                zaakInstanceId.toString(),
                "klacht",
                klachtUpdate
            )

            //then
            assertThat(temporaryDataService.getTempData(zaakInstanceId, "klacht.titel")).isEqualTo("Erpacht is te laag")

        }
    }

    @Test
    fun `should create on event and store temporary data from service method`() {
        runWithoutAuthorization {
            // Given
            val zaakInstanceId = UUID.randomUUID()

            val zaakResponse = ZaakResponse(
                url = URI.create("https://api.example.com/zaken/minimal-001"),
                uuid = zaakInstanceId,
                bronorganisatie = Rsin("002564440"),
                zaaktype = URI.create("https://api.example.com/zaaktypen/general"),
                verantwoordelijkeOrganisatie = Rsin("002564440"),
                startdatum = LocalDate.now()
            )

            temporaryDataService.removeZaakTempData(zaakInstanceId)

            val zaakCreatedEvent = ZaakCreated(zaakResponse.url.toString(), objectMapper.valueToTree(zaakResponse))

            // when
            eventPublisher.publishEvent(zaakCreatedEvent);

            // Then storing temp data should be possible
            // use service method mimicking service tasks.
            val json = """
                 {
                    "titel":"Erpacht is te laag",
                    "omschrijving":"Jouw erfpacht is in de nieuwe situatie veel te laag"
                 }
              """

            val klachtUpdate = objectMapper.readTree(json)

            temporaryDataService.storeTempData(
                zaakInstanceId.toString(),
                "klacht",
                klachtUpdate
            )


            assertThat(temporaryDataService.getTempData(zaakInstanceId, "klacht.titel")).isEqualTo("Erpacht is te laag")
        }
    }

    companion object {
        private const val PROCESS_DEFINITION_KEY = "TestProcess"
        private const val DOCUMENT_DEFINITION_KEY = "profile"
        private val logger = KotlinLogging.logger {}
    }
}
