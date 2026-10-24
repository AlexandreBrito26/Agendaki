package com.agendaki.whatsapp;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração da WhatsApp Cloud API (prefixo app.whatsapp). Tudo vem de variáveis de ambiente;
 * o token nunca deve ir para o repositório.
 */
@ConfigurationProperties(prefix = "app.whatsapp")
public record WhatsAppProperties(
        boolean enabled,
        String baseUrl,
        String apiVersion,
        String phoneNumberId,
        String accessToken,
        String templateName,
        String templateLanguage,
        String defaultCountryCode) {

    public WhatsAppProperties {
        baseUrl = orDefault(baseUrl, "https://graph.facebook.com").replaceAll("/+$", "");
        apiVersion = orDefault(apiVersion, "v23.0");
        phoneNumberId = orDefault(phoneNumberId, "");
        accessToken = orDefault(accessToken, "");
        templateName = orDefault(templateName, "agendamento_confirmado");
        templateLanguage = orDefault(templateLanguage, "pt_BR");
        defaultCountryCode = orDefault(defaultCountryCode, "55");
    }

    /** Falta credencial: com enabled=true isso vira um aviso no log em vez de uma chamada que falharia. */
    public boolean hasCredentials() {
        return !phoneNumberId.isBlank() && !accessToken.isBlank();
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
