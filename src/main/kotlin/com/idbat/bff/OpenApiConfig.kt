package com.idbat.bff

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun customOpenAPI(@Value("\${server.servlet.context-path:}") contextPath: String): OpenAPI {
        return OpenAPI()
            .addServersItem(Server().url(contextPath))
    }
}