package com.kineflex.kineflex

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class KineFlexResponse(
    @JsonProperty("success")
    val success: Boolean = false,

    @JsonProperty("message")
    val message: String? = null,

    @JsonProperty("data")
    val data: KineFlexData? = null,

    @JsonProperty("usage")
    val usage: KineFlexUsage? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KineFlexData(
    @JsonProperty("id")
    val id: Any? = null,

    @JsonProperty("name")
    val name: String? = null,

    @JsonProperty("type")
    val type: String? = null,

    @JsonProperty("url")
    val url: String? = null,

    @JsonProperty("headers")
    val headers: Map<String, String>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KineFlexUsage(
    @JsonProperty("cost")
    val cost: Int? = null,

    @JsonProperty("remaining_points")
    val remainingPoints: Int? = null,
)

/**
 * Custom exceptions representing distinct KineFlex failure states.
 */
sealed class KineFlexException(message: String) : Exception(message)

class KineFlexAuthException(message: String) : KineFlexException(message)
class KineFlexNotFoundException(message: String) : KineFlexException(message)
class KineFlexQuotaException(message: String) : KineFlexException(message)
class KineFlexServerException(message: String) : KineFlexException(message)
