package com.ritense.temporarydata

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "temporary_data")

data class ZaakTemporaryData (

    @Id
    @Column(name = "zaak_uuid")
    val zaakUUID: UUID,

    @Convert(converter = MapJsonConverter::class)
    @Column(name = "map_data")
    val mapData: MutableMap<String, Any?>
)
