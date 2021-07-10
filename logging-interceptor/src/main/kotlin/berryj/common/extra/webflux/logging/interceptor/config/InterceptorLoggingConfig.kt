package berryj.common.extra.webflux.logging.interceptor.config

import org.apache.logging.log4j.Level
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

val WEBFLUX_EXCLUDE_URL_DEFAULT = listOf(
    "/**/health",
    "/**/info",
    "/**/metrics",
    "/**/actuator*/**"
)

@Configuration
@ConfigurationProperties("logging.webflux.intercept")
data class InterceptorLoggingConfig(
    var level: Level = Level.INFO,
    var enable: Boolean = true,
    var excludeUrls: MutableList<String> = WEBFLUX_EXCLUDE_URL_DEFAULT.toMutableList(),
    var request: LoggingRequestConfig = LoggingRequestConfig(),
    var response: LoggingResponseConfig = LoggingResponseConfig()
)

data class LoggingRequestConfig(
    var enable: Boolean = true,
    var enableHeader: Boolean = true,
    var enableParameter: Boolean = true,
    var enableBody: Boolean = true,
    var excludeHeaders: MutableList<String> = mutableListOf(),
    var limitBody: Int = 0

)

data class LoggingResponseConfig(
    var enable: Boolean = true,
    var enableHeader: Boolean = true,
    var enableBody: Boolean = true,
    var excludeHeaders: MutableList<String> = mutableListOf(),
    var limitBody: Int = 0
)