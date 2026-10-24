package com.agendaki.whatsapp;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(WhatsAppProperties.class)
public class WhatsAppConfig {

    /** Cliente HTTP só da Cloud API, com timeouts curtos: o envio roda em segundo plano e não pode ficar preso. */
    @Bean
    RestClient whatsAppRestClient(RestClient.Builder builder, WhatsAppProperties props) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);
        return builder.baseUrl(props.baseUrl()).requestFactory(factory).build();
    }
}
