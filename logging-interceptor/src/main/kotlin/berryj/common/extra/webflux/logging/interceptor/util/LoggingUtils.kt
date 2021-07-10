package berryj.common.extra.webflux.logging.interceptor.util

import berryj.common.extra.webflux.logging.interceptor.filter.LoggingWebFilter
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.springframework.http.HttpHeaders
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.util.AntPathMatcher
import org.springframework.util.MultiValueMap

object LoggingUtils {

    private val log = LogManager.getLogger(LoggingWebFilter::class.java)
    private val objectMapper = jacksonObjectMapper()
    private val antPathMatcher: AntPathMatcher = AntPathMatcher()
    private var excludeLowerRequestHeaders: List<String>? = null
    private var excludeLowerResponseHeaders: List<String>? = null

    fun isExcludeUrl(requestPath: String, excludeUrls: MutableList<String>) =
        excludeUrls.find { urlString -> antPathMatcher.match(urlString, requestPath) }?.isNotEmpty() ?: false

    fun logRequest(
        level: Level = Level.INFO,
        enableHeader: Boolean = true,
        enableParameter: Boolean = true,
        excludeHeaders: MutableList<String> = mutableListOf(),
        enableBody: Boolean = true,
        limitBody: Int = 0,
        request: ServerHttpRequest,
        body: String? = null
    ) {
        StringBuilder().also { sb ->
            if (enableHeader && !request.headers.isNullOrEmpty()) {
                sb.append(" header=[").append(headerString(request.headers, excludeHeaders, true)).append("]")
            }
            if (enableParameter && !request.queryParams.isNullOrEmpty()) {
                sb.append(" parameters=[").append(parameterString(request.queryParams)).append("]")
            }
            if (enableBody && !body.isNullOrEmpty()) {
                sb.append(" body=[").append(transformBodyString(body, limitBody)).append("]")
            }
            log.log(level, "REQUEST path=[{}] method=[{}]{}", request.path.value(), request.method, sb.toString())
        }
    }

    fun logResponse(
        level: Level = Level.INFO,
        enableHeader: Boolean = true,
        excludeHeaders: MutableList<String> = mutableListOf(),
        enableBody: Boolean = true,
        limitBody: Int = 0,
        response: ServerHttpResponse,
        body: String? = null
    ) {
        StringBuilder().also { sb ->
            if (enableHeader && !response.headers.isNullOrEmpty()) {
                sb.append(" header=[").append(headerString(response.headers, excludeHeaders, false)).append("]")
            }
            if (enableBody && !body.isNullOrEmpty()) {
                sb.append(" body=[").append(transformBodyString(body, limitBody)).append("]")
            }
            log.log(level, "RESPONSE status=[{}]{}", response.rawStatusCode, sb.toString())
        }
    }

    private fun transformBodyString(body: String, limitBody: Int): String {
        return objectMapper.readValue<HashMap<String, Any>>(body).let { mapBody ->
            var bodyString = objectMapper.writeValueAsString(mapBody)
            if (limitBody != 0) {
                bodyString = bodyString.replaceRange(limitBody, bodyString.length - 1, "...")
            }
            bodyString
        }
    }

    private fun parameterString(parameters: MultiValueMap<String, String>) = HashMap<String, String>().apply {
        parameters.forEach { (key, value) ->
            this[key] = value[0]
        }
    }.let {
        objectMapper.writeValueAsString(it)
    }

    private fun headerString(httpHeaders: HttpHeaders, excludeHeaders: MutableList<String>, isRequest: Boolean) =
        HashMap<String, Any>().apply {
            httpHeaders.keys.filterNot {
                if (isRequest) {
                    if (excludeLowerRequestHeaders == null && !excludeHeaders.isNullOrEmpty())
                        excludeLowerRequestHeaders = excludeHeaders.map { header -> header.toLowerCase() }
                    excludeLowerRequestHeaders?.contains(it.toLowerCase()) ?: false
                } else {
                    if (excludeLowerResponseHeaders == null && !excludeHeaders.isNullOrEmpty())
                        excludeLowerResponseHeaders = excludeHeaders.map { header -> header.toLowerCase() }
                    excludeLowerResponseHeaders?.contains(it.toLowerCase()) ?: true
                }

            }.forEach { key ->
                this[key] = httpHeaders.getValue(key)[0]
            }
        }.let {
            objectMapper.writeValueAsString(it)
        }


}