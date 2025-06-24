package com.ritense.temporarydata

import com.ritense.valtimo.contract.annotation.ProcessBean
import org.springframework.transaction.annotation.Transactional
import java.util.*

open class TemporaryDataServiceImpl(
    val reposistory: TemporaryDataRepository
): TemporaryDataService {

    @Transactional
    override fun createOrUpdateTempData(zaakUUID: UUID, zaakId: String, tempData: Map<String, Any?>) {
        reposistory.save(ZaakTemporaryData(zaakUUID, zaakId, tempData.toMutableMap()))
    }

    @Transactional
    override fun createTempData(zaakUUID: UUID, zaakId: String) {
        reposistory.save(ZaakTemporaryData(zaakUUID, zaakId, mutableMapOf()))
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

}
