package com.examinai.config;

import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Ollama HTTP client timeouts for {@code /api/chat}. CPU-bound inference (e.g. DeepSeek R1 8B) with
 * large PR diffs often exceeds 2 minutes; the read timeout must cover full generation or the client
 * aborts and Ollama logs HTTP 500 after the same duration.
 */
@Configuration
public class AIConfig {

    @Bean
    public OllamaApi ollamaApi(
        @Value("${spring.ai.ollama.base-url}") String baseUrl,
        @Value("${examinai.ai.ollama-read-timeout-ms:900000}") int readTimeoutMs,
        @Value("${examinai.ai.ollama-connect-timeout-ms:10000}") int connectTimeoutMs
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        RestClient.Builder restClientBuilder = RestClient.builder().requestFactory(factory);
        return OllamaApi.builder()
            .baseUrl(baseUrl)
            .restClientBuilder(restClientBuilder)
            .build();
    }
}
