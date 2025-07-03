package com.ritense.temporarydata

import com.fasterxml.jackson.databind.ObjectMapper
import com.jayway.jsonpath.JsonPath
import com.ritense.document.domain.impl.request.NewDocumentRequest
import com.ritense.authorization.AuthorizationContext.Companion.runWithoutAuthorization
import com.ritense.document.service.impl.JsonSchemaDocumentService
import com.ritense.form.repository.FormDefinitionRepository
import com.ritense.form.service.PrefillFormService
import com.ritense.zakenapi.ZakenApiAuthentication
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer


import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.client.RestClient
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono
import kotlin.test.Ignore

//TODO fix test
@Ignore
class TemporaryDataValueResolverFactoryIT @Autowired constructor(
    private val documentService: JsonSchemaDocumentService,
    private val formDefinitionRepository: FormDefinitionRepository,
    private val prefillFormService: PrefillFormService,
    ): BaseIntegrationTest() {

    @Autowired
    lateinit var objectMapper: ObjectMapper

  //  lateinit var server: MockWebServer

    @BeforeEach
    internal fun setUp() {
      //  server = MockWebServer()
        setupMockZakenApiServer()
    //    server.start(port = 56273)
    }

    @AfterEach
    internal fun tearDown() {
       // server.shutdown()
    }


 //   @Test
    fun `should prefill form with data from the Temporary Data repo`() {
        runWithoutAuthorization {
            val documentId = documentService.createDocument(
                NewDocumentRequest("profile", objectMapper.createObjectNode())
            ).resultingDocument().get().id?.id

            val formDefinition = formDefinitionRepository.findByName("form-with-temporarydata-fields").get()
            val prefilledFormDefinition = prefillFormService.getPrefilledFormDefinition(
                formDefinition.id!!,
                documentId
            )
            assertThat(
                JsonPath.read<List<String>>(
                    prefilledFormDefinition.asJson().toString(),
                    "$.components[?(@.properties.sourceKey=='zaak:identificatie')].defaultValue"
                ).toString()
            ).isEqualTo("""["ZK2023-00001"]""")
        }
    }

    private fun setupMockZakenApiServer() {
      //  server.dispatcher = object : Dispatcher() {
//            @Throws(InterruptedException::class)
//            override fun dispatch(request: RecordedRequest): MockResponse {
//                val response = when (request.requestLine) {
//                    "GET /zaken/57f66ff6-db7f-43bc-84ef-6847640d3609 HTTP/1.1" -> getZaakRequest()
//                    else -> MockResponse().setResponseCode(404)
//                }
//                return response
//            }
//        }
    }

    private fun getZaakRequest(): MockResponse {
        val body = """
            {
                "url": "http://localhost:56273/zaken/a6b63eb5-cc92-4f4b-ba53-9c145133166b",
                "uuid": "a6b63eb5-cc92-4f4b-ba53-9c145133166b",
                "identificatie": "ZK2023-00001",
                "bronorganisatie": "104978119",
                "omschrijving": "Test",
                "toelichting": "",
                "zaaktype": "http://localhost:56273/catalogi/e02753ba-9055-11ee-b9d1-0242ac120002",
                "registratiedatum": "2023-03-22",
                "verantwoordelijkeOrganisatie": "104978119",
                "startdatum": "2023-03-22",
                "einddatum": null,
                "einddatumGepland": "2023-05-17",
                "uiterlijkeEinddatumAfdoening": null,
                "publicatiedatum": null,
                "communicatiekanaal": "",
                "productenOfDiensten": [],
                "vertrouwelijkheidaanduiding": "openbaar",
                "betalingsindicatie": "",
                "betalingsindicatieWeergave": "",
                "laatsteBetaaldatum": null,
                "zaakgeometrie": null,
                "verlenging": null,
                "opschorting": {
                    "indicatie": false,
                    "reden": ""
                },
                "selectielijstklasse": "",
                "hoofdzaak": null,
                "deelzaken": [],
                "relevanteAndereZaken": [],
                "eigenschappen": [],
                "rollen": [],
                "status": null,
                "zaakinformatieobjecten": [],
                "zaakobjecten": [],
                "kenmerken": [],
                "archiefnominatie": "blijvend_bewaren",
                "archiefstatus": "nog_te_archiveren",
                "archiefactiedatum": null,
                "resultaat": null,
                "opdrachtgevendeOrganisatie": "",
                "processobjectaard": "",
                "resultaattoelichting": "",
                "startdatumBewaartermijn": null
            },
        """.trimIndent()
        return mockResponse(body)
    }

    class TestAuthentication : ZakenApiAuthentication {
        override fun applyAuth(builder: RestClient.Builder): RestClient.Builder {
            return builder
        }

        override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> {
            return next.exchange(request)
        }
    }

    companion object {
        private const val PROCESS_DEFINITION_KEY = "zaken-api-plugin"
        private const val DOCUMENT_DEFINITION_KEY = "profile"
        private const val INFORMATIE_OBJECT_URL = "http://informatie.object.url"
    }
}
