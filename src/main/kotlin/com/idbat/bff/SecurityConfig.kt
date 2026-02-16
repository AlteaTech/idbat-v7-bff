package com.idbat.bff

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
class SecurityConfig {

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() } // Désactivé pour les APIs REST stateless
            .authorizeExchange { exchanges ->
                exchanges
                    // Autoriser Swagger UI et les docs API sans auth
                    .pathMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/front/v3/api-docs/**" ,
                        "/front/api/auth/login"
                    ).permitAll()
                    // Autoriser les endpoints de santé (Actuator)
                    .pathMatchers("/actuator/**").permitAll()
                    // Autoriser l'endpoint de login (si géré par le backend via la gateway)
                    .pathMatchers("/api-front/api/auth/login").permitAll() 
                    // Tout le reste nécessite une authentification
                    .anyExchange().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { } // Active la validation JWT standard
            }
            .build()
    }
}