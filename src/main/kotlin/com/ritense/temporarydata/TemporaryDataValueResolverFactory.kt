package com.ritense.temporarydata

import com.ritense.processdocument.domain.impl.CamundaProcessInstanceId
import com.ritense.processdocument.service.ProcessDocumentService
import com.ritense.valueresolver.ValueResolverFactory
import com.ritense.zakenapi.link.ZaakInstanceLinkService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.camunda.bpm.engine.delegate.VariableScope
import java.util.*
import java.util.function.Function


open class TemporaryDataValueResolverFactory (
    private val temporaryDataService: TemporaryDataService,
    private val processDocumentService: ProcessDocumentService,
    private val zaakInstanceLinkService: ZaakInstanceLinkService,
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

        temporaryDataService.createOrUpdateTempData(zaakInstanceLink.zaakInstanceId.toString(), values)
    }

    override fun supportedPrefix(): String {
       return PREFIX
    }

    private fun getZaakTempData(requestedValue: String, documentId: String): Any? {
        val zaakInstanceLink = zaakInstanceLinkService.getByDocumentId(UUID.fromString(documentId))
        logger.debug { "getting zaakdata for zaak: ${zaakInstanceLink.zaakInstanceId}" }
        return temporaryDataService.getTempData(zaakInstanceLink.zaakInstanceId, requestedValue)
    }

    companion object {
        private val logger = KotlinLogging.logger {}
        const val PREFIX = "tzd" // temporary zaak data
    }
}
