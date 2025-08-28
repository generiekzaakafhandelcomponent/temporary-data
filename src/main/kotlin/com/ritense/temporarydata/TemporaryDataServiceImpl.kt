/*
 * Copyright 2015-2025. Ritense BV, the Netherlands.
 *
 *  Licensed under EUPL, Version 1.2 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.ritense.temporarydata


import com.fasterxml.jackson.databind.ObjectMapper
import com.ritense.temporarydata.TemporaryDataValueResolverFactory.Companion.SEPARATOR
import com.ritense.temporarydata.TemporaryDataValueResolverFactory.Companion.FORM_SEPARATOR
import com.ritense.zakenapi.domain.ZaakResponse
import com.ritense.zakenapi.event.ZaakCreated
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*
import kotlin.collections.LinkedHashMap



@Component
class TemporaryDataServiceImpl(
    val reposistory: TemporaryDataRepository,
    val objectMapper: ObjectMapper
): TemporaryDataService {

    @Transactional
    override fun createOrUpdateTempData(zaakUUID: String, tempData: Map<String, Any?>) {
        logger.debug { "writing data ${tempData}" }

        if(reposistory.existsByZaakUUID(UUID.fromString(zaakUUID))){
            var data = reposistory.findByZaakUUID(UUID.fromString(zaakUUID)).get()
            var mapData = data.mapData

            var workMap = mutableMapOf<String, Any?>()
            tempData.toMutableMap().keys.forEach {
                var key = it.replace(".", SEPARATOR)
                var value = tempData.get(key)
                setNestedValue(workMap, key.split(SEPARATOR), value)
            }

            mapData.putAll(workMap)

            logger.debug { "writing merged map data ${mapData}" }

            reposistory.save(data)
        }
        else {
            var map = mutableMapOf<String, Any?>()
            tempData.toMutableMap().keys.forEach {
                var key = it.replace(FORM_SEPARATOR, SEPARATOR)
                var value = tempData.get(key)
                setNestedValue(map, key.split(SEPARATOR), value)
            }

            reposistory.save(ZaakTemporaryData(UUID.fromString(zaakUUID), map))
        }
    }

    @Transactional
    override fun createTempData(zaakUUID: String) {
        reposistory.save(ZaakTemporaryData(UUID.fromString(zaakUUID), mutableMapOf()))
    }

    @Transactional
    override fun storeTempData(zaakUUID: String, key: String, tempData:Any?) {
        var data = reposistory.findByZaakUUID(UUID.fromString(zaakUUID)).get()
        setNestedValue(data.mapData, key.split(SEPARATOR), tempData)
        reposistory.save(data)
    }

    @Transactional(readOnly = true)
    override fun getTempData(zaakUUID: UUID, key: String): Any? {
        var data = reposistory.findByZaakUUID(zaakUUID).get()

        var formattedKey = key.replace(FORM_SEPARATOR, SEPARATOR)

        if(!formattedKey.contains(SEPARATOR)) {
            return data.mapData.get(formattedKey)
        }
        return getNestedValue(data.mapData, formattedKey.split(SEPARATOR))
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

    private fun getNestedValue(map: Any?, keys: List<String>): Any? {
        if (keys.isEmpty() || map == null) return map

        val currentKey = keys.first()
        val remainingKeys = keys.drop(1)

        val nextValue = when (map) {
            is Map<*, *> -> map[currentKey]
            else -> null
        }

        return if (remainingKeys.isEmpty()) {
            nextValue
        } else {
            getNestedValue(nextValue, remainingKeys)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun setNestedValue(map: MutableMap<String, Any?>, keys: List<String>, value: Any?) {
        if (keys.isEmpty()) return

        val currentKey = keys.first()
        val remainingKeys = keys.drop(1)

        if (remainingKeys.isEmpty()) {
            if(map[currentKey] is LinkedHashMap<*, *> && value is LinkedHashMap<*, *>) {
                var currentValue = map[currentKey] as LinkedHashMap<String, Any?>
                var updateValue  = value as LinkedHashMap<String, Any>
                currentValue.putAll(updateValue)
            }
            else {
                map.put(currentKey, value)
            }
        } else {
            val nextMap = when (val existing = map[currentKey]) {
                is MutableMap<*, *> -> existing as MutableMap<String, Any?>
                null -> {
                    val newMap = mutableMapOf<String, Any?>()
                    map[currentKey] = newMap
                    newMap
                }
                else -> {
                    val newMap = mutableMapOf<String, Any?>()
                    map[currentKey] = newMap
                    newMap
                }
            }
            setNestedValue(nextMap, remainingKeys, value)
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }

}
