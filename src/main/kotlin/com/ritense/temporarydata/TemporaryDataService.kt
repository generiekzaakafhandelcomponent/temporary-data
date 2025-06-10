package com.ritense.temporarydata

import java.util.UUID

interface TemporaryDataService {

  fun storeTempData(zaakUUID: UUID, key: String, tempData: Any)
  fun storeTempData(zaakId: String, key: String, tempData: Any)

  fun getTempData(zaakUUId: UUID, key: String): Any
  fun getTempData(zaakId: String, key: String): Any
  fun removeZaakTempData(zaakUUID: UUID);

}
