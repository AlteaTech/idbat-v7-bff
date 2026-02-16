package com.idbat.bff

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
class SecurityConfig(
    private val customAuthenticationManager: ReactiveAuthenticationManager
) {

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/api-front/v3/api-docs/**"
                    ).permitAll()
                    .pathMatchers("/actuator/**").permitAll()
                    .pathMatchers("/api-front/api/auth/login").permitAll()
                    .anyExchange().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                // On utilise notre CustomAuthenticationManager au lieu du JwtDecoder standard
                oauth2.jwt { jwt ->
                    jwt.authenticationManager(customAuthenticationManager)
                }
            }
            .build()
    }
}