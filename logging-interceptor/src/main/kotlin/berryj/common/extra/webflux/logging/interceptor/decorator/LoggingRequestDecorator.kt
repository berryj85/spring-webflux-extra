package berryj.common.extra.webflux.logging.interceptor.decorator

import berryj.common.extra.webflux.logging.interceptor.config.InterceptorLoggingConfig
import berryj.common.extra.webflux.logging.interceptor.util.LoggingUtils
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpRequestDecorator
import org.springframework.util.StreamUtils
import reactor.core.publisher.Flux
import java.io.ByteArrayOutputStream
import java.nio.channels.Channels
import java.nio.charset.Charset

class LoggingRequestDecorator(private val config: InterceptorLoggingConfig, delegate: ServerHttpRequest) :
    ServerHttpRequestDecorator(delegate) {
    private var body: String? = null
    override fun getBody(): Flux<DataBuffer> {
        return super.getBody().doOnNext { dataBuffer ->
            ByteArrayOutputStream().also { outputStream ->
                Channels.newChannel(outputStream).write(dataBuffer.asByteBuffer().asReadOnlyBuffer())
                body = StreamUtils.copyToString(outputStream, Charset.forName("UTF-8"))
                outputStream.close()
            }
        }.doOnComplete {
            if (delegate.headers.contentLength > 0) {
                LoggingUtils.logRequest(
                    config.level,
                    config.request.enableHeader,
                    config.request.enableParameter,
                    config.request.excludeHeaders,
                    config.request.enableBody,
                    config.request.limitBody,
                    delegate,
                    body
                )
            }

        }
    }
}