package com.ritense.temporarydata

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface TemporaryDataRepository : JpaRepository<ZaakTemporaryData, UUID> {

    fun findByZaakUUID(zaakUUID: UUID): Optional<ZaakTemporaryData>

    fun existsByZaakUUID(zaakUUID: UUID): Boolean

    fun deleteByZaakUUID(zaakUUID: UUID)

    fun findByZaakId(zaakId: String): Optional<ZaakTemporaryData>

    fun existsByZaakId(zaakId: String): Boolean

    fun deleteByZaakId(zaakId: String)
}
