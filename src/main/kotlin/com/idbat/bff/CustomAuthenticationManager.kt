package com.idbat.bff

import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class CustomAuthenticationManager(
    private val webClientBuilder: WebClient.Builder,
    @Value("\${app.auth.validate-url}") private val validateUrl: String
) : ReactiveAuthenticationManager {

    override fun authenticate(authentication: Authentication): Mono<Authentication> {
        val token = (authentication as BearerTokenAuthenticationToken).token

        return webClientBuilder.build()
            .post()
            .uri(validateUrl)
            .bodyValue(mapOf("token" to token))
            .retrieve()
            .bodyToMono(ValidationResponse::class.java)
            .flatMap { response ->
                if (response.valid) {
                    val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
                    val auth: Authentication = UsernamePasswordAuthenticationToken(response.username, token, authorities)
                    Mono.just(auth)
                } else {
                    Mono.empty()
                }
            }
            .onErrorResume { Mono.empty() }
    }

    data class ValidationResponse(val valid: Boolean, val username: String?)
}