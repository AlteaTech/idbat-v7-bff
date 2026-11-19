package com.idbat.bff

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import org.springframework.web.util.pattern.PathPatternParser
import reactor.core.publisher.Mono

/**
 * Quand Swagger est desactive (SWAGGER_ENABLED absent ou false), springdoc coupe la
 * doc du BFF, mais la route gateway /front/ ** relaierait encore la doc du backend
 * (/front/v3/api-docs, /front/swagger-ui...). Ce filtre repond 404 sur ces chemins
 * avant tout routage, avec ou sans X-Forwarded-Prefix.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class SwaggerBlockFilter(
    @Value("\${springdoc.api-docs.enabled:false}") private val swaggerEnabled: Boolean
) : WebFilter {

    // Racine (BFF) et /front (backend relaye). /webjars/** est indispensable : meme avec
    // springdoc desactive, Spring Boot sert les fichiers statiques du webjar swagger-ui,
    // et index.html y affiche alors l'exemple "Swagger Petstore" par defaut.
    private val blockedPatterns = listOf("", "/front").flatMap { base ->
        listOf(
            "$base/v3/api-docs*",
            "$base/v3/api-docs*/**",
            "$base/swagger-ui*",
            "$base/swagger-ui*/**",
            "$base/webjars/**",
        )
    }.map { PathPatternParser.defaultInstance.parse(it) }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        if (swaggerEnabled) {
            return chain.filter(exchange)
        }
        val path = exchange.request.path
        val blocked = blockedPatterns.any {
            it.matches(path.pathWithinApplication()) || it.matches(path)
        }
        if (!blocked) {
            return chain.filter(exchange)
        }
        exchange.response.statusCode = HttpStatus.NOT_FOUND
        return exchange.response.setComplete()
    }
}
