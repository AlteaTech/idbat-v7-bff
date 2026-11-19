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

/** SWAGGER_ENABLED absent : aucune doc OpenAPI ne doit sortir, ni du BFF ni du backend. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SwaggerDisabledTest {

    @Autowired
    lateinit var client: WebTestClient

    private fun expect404(uri: String, prefix: String? = null) {
        val req = client.get().uri(uri)
        if (prefix != null) req.header("X-Forwarded-Prefix", prefix)
        req.exchange().expectStatus().isNotFound
    }

    @Test
    fun `doc du BFF desactivee`() {
        expect404("/v3/api-docs")
        expect404("/v3/api-docs/swagger-config")
        expect404("/swagger-ui.html")
        expect404("/swagger-ui/index.html")
        expect404("/webjars/swagger-ui/index.html")
        expect404("/webjars/swagger-ui/swagger-initializer.js")
        expect404("/swagger-ui/index.html", prefix = "/back-bff")
    }

    @Test
    fun `doc du backend non relayee par le gateway`() {
        expect404("/front/v3/api-docs")
        expect404("/front/v3/api-docs/")
        expect404("/front/v3/api-docs.yaml")
        expect404("/front/v3/api-docs/swagger-config")
        expect404("/front/v3/api-docs;x=1")
        expect404("/front/swagger-ui.html")
        expect404("/front/swagger-ui/index.html")
        expect404("/front/v3/api-docs", prefix = "/back-bff")
    }

    @Test
    fun `les autres routes backend passent toujours`() {
        client.get().uri("/front/api/ping")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java).isEqualTo("path=/api/ping")
    }

    companion object {
        private val backend: DisposableServer = HttpServer.create().port(0)
            .handle { request, response ->
                response.sendString(reactor.core.publisher.Mono.just("path=${request.uri()}"))
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
