package com.ritense.temporarydata

import com.ritense.valueresolver.ValueResolverFactory
import org.camunda.bpm.engine.delegate.VariableScope
import java.util.function.Function

open class TemporaryDataValueResolverFactory : ValueResolverFactory {
    override fun createResolver(documentId: String): Function<String, Any?> {
        TODO("Not yet implemented")
    }

    override fun createResolver(processInstanceId: String, variableScope: VariableScope): Function<String, Any?> {
        TODO("Not yet implemented")
    }

    override fun handleValues(processInstanceId: String, variableScope: VariableScope?, values: Map<String, Any?>) {
        TODO("Not yet implemented")
    }

    override fun supportedPrefix(): String {
       return PREFIX
    }

    companion object {
        const val PREFIX = "tzd" // temporary zaak data
    }
}
