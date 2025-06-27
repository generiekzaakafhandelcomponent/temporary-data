package com.ritense.temporarydata

import com.fasterxml.jackson.databind.ObjectMapper
import com.ritense.valtimo.contract.annotation.ProcessBean
import com.ritense.zakenapi.domain.ZaakInstanceLink
import com.ritense.zakenapi.domain.ZaakResponse
import com.ritense.zakenapi.event.ZaakCreated
import org.springframework.context.event.EventListener
import org.springframework.transaction.annotation.Transactional
import java.util.*

open class TemporaryDataServiceImpl(
    val reposistory: TemporaryDataRepository,
    val objectMapper: ObjectMapper
): TemporaryDataService {

    @Transactional
    override fun createOrUpdateTempData(zaakUUID: String, zaakId: String, tempData: Map<String, Any?>) {
        reposistory.save(ZaakTemporaryData(UUID.fromString(zaakUUID), zaakId, tempData.toMutableMap()))
    }

    @Transactional
    override fun createTempData(zaakUUID: String, zaakId: String) {
        reposistory.save(ZaakTemporaryData(UUID.fromString(zaakUUID), zaakId, mutableMapOf()))
    }

    @Transactional
    override fun storeTempData(zaakUUID: UUID, key: String, tempData:Any?) {
        var data = reposistory.findByZaakUUID(zaakUUID).get()
        data.mapData.put(key, tempData)
        reposistory.save(data)
    }

    @Transactional
    override fun storeTempData(zaakId: String, key: String, tempData: Any?) {
        var data = reposistory.findByZaakId(zaakId).get()
        data.mapData.put(key, tempData)
        reposistory.save(data)
    }

    @Transactional(readOnly = true)
    override fun getTempData(zaakUUID: UUID, key: String): Any? {
        var data = reposistory.findByZaakUUID(zaakUUID).get()
        return data.mapData.get(key)

    }

    @Transactional(readOnly = true)
    override fun getTempData(zaakId: String, key: String): Any? {
        var data = reposistory.findByZaakId(zaakId).get()
        return data.mapData.get(key)
    }

    @Transactional
    override fun removeZaakTempData(zaakUUID: UUID) {
        reposistory.deleteByZaakUUID(zaakUUID)
    }

    @EventListener(ZaakCreated::class)
    fun createTempDataMap(event: ZaakCreated) {
        val zaakResponse = objectMapper.readValue(event.result.toString(), ZaakResponse::class.java)
        val emptyTempData = ZaakTemporaryData(zaakResponse.uuid, zaakResponse.identificatie as String, mutableMapOf())
        reposistory.save(emptyTempData);
    }

}
