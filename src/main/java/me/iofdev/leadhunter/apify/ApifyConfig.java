package me.iofdev.leadhunter.apify;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
class ApifyConfig {

    @Bean
    ApifyClient apifyClient(RestClient.Builder builder, ApifyProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        // A status request blocks for up to pollWait on Apify's side, so the read timeout must be longer.
        requestFactory.setReadTimeout(properties.pollWait().plusSeconds(30));
        RestClient restClient = builder.baseUrl(properties.baseUrl()).requestFactory(requestFactory).build();
        return new ApifyClient(restClient, properties.token());
    }
}
