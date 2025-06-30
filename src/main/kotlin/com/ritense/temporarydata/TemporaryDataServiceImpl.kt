package com.ritense.temporarydata

import com.fasterxml.jackson.databind.ObjectMapper
import com.ritense.zakenapi.domain.ZaakResponse
import com.ritense.zakenapi.event.ZaakCreated
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.transaction.annotation.Transactional
import java.util.*

open class TemporaryDataServiceImpl(
    val reposistory: TemporaryDataRepository,
    val objectMapper: ObjectMapper
): TemporaryDataService {

    @Transactional
    override fun createOrUpdateTempData(zaakUUID: String, tempData: Map<String, Any?>) {
        logger.debug { "writing data ${tempData}" }

        if(reposistory.existsByZaakUUID(UUID.fromString(zaakUUID))){
            var data = reposistory.findByZaakUUID(UUID.fromString(zaakUUID)).get()
            var mapData = data.mapData
            mapData.putAll(tempData)

            logger.debug { "writing merged map data ${mapData}" }

            reposistory.save(data)
        }
        else {
            reposistory.save(ZaakTemporaryData(UUID.fromString(zaakUUID), tempData.toMutableMap()))
        }
    }

    @Transactional
    override fun createTempData(zaakUUID: String) {
        reposistory.save(ZaakTemporaryData(UUID.fromString(zaakUUID), mutableMapOf()))
    }

    @Transactional
    override fun storeTempData(zaakUUID: String, key: String, tempData:Any?) {
        var data = reposistory.findByZaakUUID(UUID.fromString(zaakUUID)).get()
        data.mapData.put(key, tempData)
        reposistory.save(data)
    }

    @Transactional(readOnly = true)
    override fun getTempData(zaakUUID: UUID, key: String): Any? {
        var data = reposistory.findByZaakUUID(zaakUUID).get()
        return data.mapData.get(key)

    }

    @Transactional
    override fun removeZaakTempData(zaakUUID: UUID) {
        reposistory.deleteByZaakUUID(zaakUUID)
    }

    @EventListener(ZaakCreated::class)
    fun createTempDataMap(event: ZaakCreated) {
        val zaakResponse = objectMapper.readValue(event.result.toString(), ZaakResponse::class.java)
        val emptyTempData = ZaakTemporaryData(zaakResponse.uuid, mutableMapOf())
        reposistory.save(emptyTempData);
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }

}
