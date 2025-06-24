package com.ritense.temporarydata.autoconfigure

import com.ritense.valtimo.contract.config.LiquibaseMasterChangeLogLocation
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import javax.sql.DataSource

@AutoConfiguration
@ConditionalOnMissingBean(DataSource::class)
class TemporaryDataLiquibaseAutoConfiguration {

    @Order(Ordered.HIGHEST_PRECEDENCE + 30)
    @Bean
    @ConditionalOnMissingBean(name = ["temporaryDataLiquibaseMasterChangeLogLocation"])
    fun temporaryDataLiquibaseMasterChangeLogLocation(): LiquibaseMasterChangeLogLocation {
        return LiquibaseMasterChangeLogLocation("config/liquibase/temporarydata-master.xml")
    }
}
