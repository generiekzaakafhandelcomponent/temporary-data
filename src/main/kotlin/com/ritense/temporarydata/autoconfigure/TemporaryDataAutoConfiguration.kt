package com.ritense.temporarydata.autoconfigure

import com.fasterxml.jackson.databind.ObjectMapper
import com.ritense.plugin.service.PluginService
import com.ritense.processdocument.service.ProcessDocumentService
import com.ritense.temporarydata.*
import com.ritense.valtimo.contract.annotation.ProcessBean
import com.ritense.zakenapi.link.ZaakInstanceLinkService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@AutoConfiguration
@EnableJpaRepositories(basePackages = ["com.ritense.temporarydata"])
@EntityScan(basePackages = ["com.ritense.temporarydata"])
class TemporaryDataAutoConfiguration {

    @ProcessBean
    @Bean
    @ConditionalOnMissingBean(TemporaryDataService::class)
    fun temporaryDataService (
        reposistory: TemporaryDataRepository,
        objectMapper: ObjectMapper
    ): TemporaryDataService  {
        return TemporaryDataServiceImpl(reposistory, objectMapper)
    }

    @Bean
    @ConditionalOnMissingBean(TemporaryDataValueResolverFactory::class)
    fun tempDataValueResolverFactory(
        temporaryDataService: TemporaryDataService,
        processDocumentService: ProcessDocumentService,
        zaakInstanceLinkService: ZaakInstanceLinkService,
        pluginService: PluginService
    ):TemporaryDataValueResolverFactory {
        return TemporaryDataValueResolverFactory(temporaryDataService, processDocumentService, zaakInstanceLinkService, pluginService)
    }
}
