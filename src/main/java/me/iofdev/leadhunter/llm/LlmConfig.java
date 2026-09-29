package me.iofdev.leadhunter.llm;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
class LlmConfig {

    @Bean
    LlmClient llmClient(RestClient.Builder builder, LlmProperties properties, LlmCallRepository calls) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.timeout());
        RestClient restClient = builder.clone().baseUrl(properties.baseUrl()).requestFactory(requestFactory).build();
        return new RecordingLlmClient(
                new OpenAiCompatibleLlmClient(restClient, properties.apiKey(), properties.model()), calls);
    }
}
