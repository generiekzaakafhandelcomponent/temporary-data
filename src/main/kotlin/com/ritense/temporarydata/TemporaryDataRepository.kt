package com.ritense.temporarydata

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TemporaryDataRepository : JpaRepository<ZaakTemporaryData, UUID> {

    fun findByZaakUUID(zaakUUID: UUID): Optional<ZaakTemporaryData>

    fun existsByZaakUUID(zaakUUID: UUID): Boolean

    fun deleteByZaakUUID(zaakUUID: UUID)

    fun findByZaakId(zaakId: String): Optional<ZaakTemporaryData>

    fun existsByZaakId(zaakId: String): Boolean

    fun deleteByZaakId(zaakId: String)


}
