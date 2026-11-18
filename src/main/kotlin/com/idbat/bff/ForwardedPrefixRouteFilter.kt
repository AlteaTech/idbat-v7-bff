package com.idbat.bff

import org.springframework.cloud.gateway.config.GatewayProperties
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import org.springframework.web.util.pattern.PathPatternParser
import reactor.core.publisher.Mono

/**
 * Derriere un proxy qui envoie X-Forwarded-Prefix (ex: /back-bff), Spring prefixe le
 * chemin de la requete et pose ce prefixe en context path. Le predicat Path et le
 * StripPrefix du gateway travaillent sur le chemin brut et ignorent le context path :
 * /front/xxx ne matche plus aucune route et le BFF repond 404.
 *
 * Pour les seuls chemins routes par le gateway, on retire donc le context path et on
 * repose le prefixe externe dans X-Forwarded-Prefix : le filtre XForwarded du gateway
 * y ajoute ensuite le sien (/front) et le backend recoit "/back-bff,/front".
 * Les autres chemins (swagger-ui, api-docs, actuator) gardent leur context path.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class ForwardedPrefixRouteFilter(gatewayProperties: GatewayProperties) : WebFilter {

    private val routePatterns = gatewayProperties.routes
        .flatMap { it.predicates }
        .filter { it.name == "Path" }
        .flatMap { it.args.values }
        .map { PathPatternParser.defaultInstance.parse(it) }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val path = exchange.request.path
        val prefix = path.contextPath().value()
        val pathWithinApplication = path.pathWithinApplication()
        if (prefix.isEmpty() || routePatterns.none { it.matches(pathWithinApplication) }) {
            return chain.filter(exchange)
        }
        val request = exchange.request.mutate()
            .contextPath("")
            .path(pathWithinApplication.value())
            .header(X_FORWARDED_PREFIX, prefix)
            .build()
        return chain.filter(exchange.mutate().request(request).build())
    }

    companion object {
        private const val X_FORWARDED_PREFIX = "X-Forwarded-Prefix"
    }
}
