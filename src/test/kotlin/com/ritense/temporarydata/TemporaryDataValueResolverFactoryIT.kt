package com.ritense.temporarydata

import com.fasterxml.jackson.databind.ObjectMapper
import com.jayway.jsonpath.JsonPath
import com.ritense.authorization.AuthorizationContext.Companion.runWithoutAuthorization
import com.ritense.document.domain.impl.request.NewDocumentRequest
import com.ritense.document.service.impl.JsonSchemaDocumentService
import com.ritense.form.repository.FormDefinitionRepository
import com.ritense.form.service.FormSubmissionService
import com.ritense.form.service.PrefillFormService
import com.ritense.processdocument.domain.impl.request.NewDocumentAndStartProcessRequest
import com.ritense.processdocument.service.ProcessDocumentService
import com.ritense.processlink.service.ProcessLinkService
import com.ritense.zakenapi.link.ZaakInstanceLinkService
import java.net.URI
import java.util.UUID
import kotlin.test.Test
import org.assertj.core.api.Assertions.assertThat
import org.camunda.bpm.engine.TaskService
import org.springframework.beans.factory.annotation.Autowired

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
    private val taskService: TaskService
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

    companion object {
        private const val PROCESS_DEFINITION_KEY = "TestProcess"
        private const val DOCUMENT_DEFINITION_KEY = "profile"
    }
}
