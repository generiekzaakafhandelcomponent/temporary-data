package com.ritense.temporarydata

import com.ritense.plugin.service.PluginService
import com.ritense.processdocument.domain.impl.CamundaProcessInstanceId
import com.ritense.processdocument.service.ProcessDocumentService
import com.ritense.valueresolver.ValueResolverFactory
import com.ritense.zakenapi.ZakenApiPlugin
import com.ritense.zakenapi.link.ZaakInstanceLinkService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.camunda.bpm.engine.delegate.VariableScope
import java.net.URI
import java.util.*
import java.util.function.Function


open class TemporaryDataValueResolverFactory (
    private val temporaryDataService: TemporaryDataService,
    private val processDocumentService: ProcessDocumentService,
    private val zaakInstanceLinkService: ZaakInstanceLinkService,
    private val pluginService: PluginService
    ): ValueResolverFactory {
    override fun createResolver(documentId: String): Function<String, Any?> {
        return Function { requestedValue ->
            getZaakTempData(requestedValue, documentId)
        }
    }

    override fun createResolver(processInstanceId: String, variableScope: VariableScope): Function<String, Any?> {
        return Function { requestedValue ->
            logger.debug { "Requested zaak object value '$requestedValue' for process $processInstanceId" }.toString()
            val documentId = processDocumentService.getDocumentId(CamundaProcessInstanceId(processInstanceId), variableScope).toString()
            getZaakTempData(requestedValue, documentId)
        }
    }

    override fun handleValues(processInstanceId: String, variableScope: VariableScope?, values: Map<String, Any?>) {
        val documentId = processDocumentService.getDocumentId(CamundaProcessInstanceId(processInstanceId), variableScope).toString()
        val zaakInstanceLink = zaakInstanceLinkService.getByDocumentId(UUID.fromString(documentId))
        val plugin = findZakenApiPlugin(zaakInstanceLink.zaakInstanceUrl)
        val zaakResponse = plugin.getZaak(zaakInstanceLink.zaakInstanceUrl)

        requireNotNull(zaakResponse.identificatie) {
            "zaakresponse  on url ${zaakResponse.url} does not contain identificatie"
        }

        temporaryDataService.createOrUpdateTempData(zaakResponse.uuid, zaakResponse.identificatie!!, values)
    }

    override fun supportedPrefix(): String {
       return PREFIX
    }

    private fun getZaakTempData(requestedValue: String, documentId: String): Any? {
        val zaakInstanceLink = zaakInstanceLinkService.getByDocumentId(UUID.fromString(documentId))
        return temporaryDataService.getTempData(zaakInstanceLink.zaakInstanceLinkId.id, requestedValue)
    }

    private fun findZakenApiPlugin(zaakUrl: URI): ZakenApiPlugin {
        val zakenApiPluginInstance = pluginService
            .createInstance(ZakenApiPlugin::class.java, ZakenApiPlugin.findConfigurationByUrl(zaakUrl))

        requireNotNull(zakenApiPluginInstance) { "No plugin configuration was found for zaak with URL $zaakUrl" }

        return zakenApiPluginInstance
    }

    companion object {
        private val logger = KotlinLogging.logger {}
        const val PREFIX = "tzd" // temporary zaak data
    }
}
