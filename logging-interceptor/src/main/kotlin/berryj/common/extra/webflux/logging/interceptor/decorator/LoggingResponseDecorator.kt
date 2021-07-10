package berryj.common.extra.webflux.logging.interceptor.decorator

import berryj.common.extra.webflux.logging.interceptor.config.InterceptorLoggingConfig
import berryj.common.extra.webflux.logging.interceptor.util.LoggingUtils
import org.reactivestreams.Publisher
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.http.server.reactive.ServerHttpResponseDecorator
import org.springframework.util.StreamUtils
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.ByteArrayOutputStream
import java.nio.channels.Channels

class LoggingResponseDecorator(private val config: InterceptorLoggingConfig, delegate: ServerHttpResponse) :
    ServerHttpResponseDecorator(delegate) {
    private var bodyString: String? = null
    override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {
        return super.writeWith(Flux.from(body).doOnNext { dataBuffer ->
            ByteArrayOutputStream().also { outputStream ->
                Channels.newChannel(outputStream).write(dataBuffer.asByteBuffer().asReadOnlyBuffer())
                bodyString = StreamUtils.copyToString(outputStream, Charsets.UTF_8)
                outputStream.close()

            }
        }).doFinally {
            if (config.response.enable) {
                LoggingUtils.logResponse(
                    config.level,
                    config.response.enableHeader,
                    config.response.excludeHeaders,
                    config.response.enableBody,
                    config.response.limitBody,
                    delegate,
                    bodyString
                )
            }
        }
    }
}