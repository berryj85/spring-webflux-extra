package berryj.common.extra.webflux.logging.interceptor.filter

import berryj.common.extra.webflux.logging.interceptor.config.InterceptorLoggingConfig
import berryj.common.extra.webflux.logging.interceptor.decorator.LoggingRequestDecorator
import berryj.common.extra.webflux.logging.interceptor.decorator.LoggingResponseDecorator
import berryj.common.extra.webflux.logging.interceptor.util.LoggingUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class LoggingWebFilter : WebFilter, Ordered {
    @Autowired
    private lateinit var config: InterceptorLoggingConfig
    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        return if (this.isFilter(exchange)) {
            exchange.mutate()
                .request(LoggingRequestDecorator(config, exchange.request))
                .response(LoggingResponseDecorator(config, exchange.response))
                .build()
        } else {
            exchange
        }.let {
            chain.filter(it)
        }.doOnSubscribe {
            if (this.isFilter(exchange) && exchange.request.headers.contentLength <= 0) {
                LoggingUtils.logRequest(
                    config.level,
                    config.request.enableHeader,
                    config.request.enableParameter,
                    config.request.excludeHeaders,
                    config.request.enableBody,
                    config.request.limitBody,
                    exchange.request
                )
            }
        }.doFinally {
            if (this.isFilter(exchange) && exchange.response.headers.contentLength <= 0) {
                LoggingUtils.logResponse(
                    config.level,
                    config.response.enableHeader,
                    config.response.excludeHeaders,
                    config.response.enableBody,
                    config.response.limitBody,
                    exchange.response
                )
            }
        }
    }

    private fun isFilter(exchange: ServerWebExchange): Boolean =
        config.enable && !LoggingUtils.isExcludeUrl(exchange.request.uri.path, config.excludeUrls)
}