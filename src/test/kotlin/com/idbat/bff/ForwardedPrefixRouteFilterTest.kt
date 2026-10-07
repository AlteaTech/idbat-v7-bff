package com.idbat.bff

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.netty.DisposableServer
import reactor.netty.http.server.HttpServer

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ForwardedPrefixRouteFilterTest {

    @Autowired
    lateinit var client: WebTestClient

    @Test
    fun `route vers le backend sans X-Forwarded-Prefix`() {
        client.get().uri("/front/v3/api-docs")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java).isEqualTo("path=/v3/api-docs prefix=/front")
    }

    @Test
    fun `route vers le backend avec X-Forwarded-Prefix`() {
        client.get().uri("/front/v3/api-docs")
            .header("X-Forwarded-Prefix", "/back-bff")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java).isEqualTo("path=/v3/api-docs prefix=/back-bff,/front")
    }

    @Test
    fun `swagger-config garde le prefixe externe`() {
        client.get().uri("/v3/api-docs/swagger-config")
            .header("X-Forwarded-Prefix", "/back-bff")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.configUrl").isEqualTo("/back-bff/v3/api-docs/swagger-config")
            .jsonPath("$.urls[0].url").isEqualTo("/back-bff/front/v3/api-docs")
    }

    companion object {
        // Faux backend : renvoie le chemin et le X-Forwarded-Prefix recus
        private val backend: DisposableServer = HttpServer.create().port(0)
            .handle { request, response ->
                val prefix = request.requestHeaders().getAll("X-Forwarded-Prefix").joinToString(",")
                response.sendString(reactor.core.publisher.Mono.just("path=${request.uri()} prefix=$prefix"))
            }
            .bindNow()

        @JvmStatic
        @DynamicPropertySource
        fun backendUri(registry: DynamicPropertyRegistry) {
            registry.add("BACKEND_URI") { "http://localhost:${backend.port()}" }
        }

        @JvmStatic
        @AfterAll
        fun stopBackend() {
            backend.disposeNow()
        }
    }
}
