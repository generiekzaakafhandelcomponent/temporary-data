package com.ritense.temporarydata

import java.util.UUID

interface TemporaryDataService {

  fun createOrUpdateTempData(zaakUUID: String, tempData: Map<String, Any?>)
  fun createTempData(zaakUUID: String)

  fun storeTempData(zaakUUID: String, key: String, tempData:Any?)
  fun getTempData(zaakUUId: UUID, key: String): Any?
  fun removeZaakTempData(zaakUUID: UUID)

}
