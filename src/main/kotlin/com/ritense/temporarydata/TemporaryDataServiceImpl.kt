package com.ritense.temporarydata

import java.util.*

open class TemporaryDataServiceImpl: TemporaryDataService {
    override fun storeTempData(zaakUUID: UUID, key: String, tempData: Any) {
        TODO("Not yet implemented")
    }

    override fun storeTempData(zaakId: String, key: String, tempData: Any) {
        TODO("Not yet implemented")
    }

    override fun getTempData(zaakUUId: UUID, key: String): Any {
        TODO("Not yet implemented")
    }

    override fun getTempData(zaakId: String, key: String): Any {
        TODO("Not yet implemented")
    }

    override fun removeZaakTempData(zaakUUID: UUID) {
        TODO("Not yet implemented")
    }
}
