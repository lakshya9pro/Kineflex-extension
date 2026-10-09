package com.kineflex.models

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

/**
 * Shared JSON serialization and deserialization utility using Jackson,
 * configured to safely ignore unknown properties and handle missing fields.
 */
object KineFlexJson {
    val mapper: ObjectMapper = jacksonObjectMapper().apply {
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
        configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
    }

    inline fun <reified T> parseSafe(jsonString: String?): T? {
        if (jsonString.isNullOrBlank()) return null
        return try {
            mapper.readValue(jsonString, T::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun stringify(value: Any?): String {
        if (value == null) return ""
        return try {
            mapper.writeValueAsString(value)
        } catch (_: Exception) {
            ""
        }
    }
}
