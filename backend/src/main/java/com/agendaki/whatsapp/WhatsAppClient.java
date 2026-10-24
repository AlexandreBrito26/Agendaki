package com.agendaki.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Chamada HTTP para a WhatsApp Cloud API (Meta). Só sabe enviar uma mensagem de template. */
@Component
public class WhatsAppClient {

    private final WhatsAppProperties props;
    private final RestClient rest;

    public WhatsAppClient(WhatsAppProperties props, RestClient whatsAppRestClient) {
        this.props = props;
        this.rest = whatsAppRestClient;
    }

    /**
     * Envia o template configurado, preenchendo as variáveis {{1}}, {{2}}... do corpo na ordem recebida.
     *
     * @param to número só com dígitos e código do país (ver {@link PhoneNumbers})
     * @return o id da mensagem (wamid...) devolvido pela Meta
     * @throws WhatsAppSendException se a API recusar ou não responder
     */
    public String sendTemplate(String to, List<String> bodyParams) {
        var parameters = bodyParams.stream()
                .map(text -> Map.of("type", "text", "text", text))
                .toList();

        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "template",
                "template", Map.of(
                        "name", props.templateName(),
                        "language", Map.of("code", props.templateLanguage()),
                        "components", List.of(Map.of("type", "body", "parameters", parameters))));

        try {
            JsonNode response = rest.post()
                    .uri("/{version}/{phoneNumberId}/messages", props.apiVersion(), props.phoneNumberId())
                    .header("Authorization", "Bearer " + props.accessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode id = response == null ? null : response.path("messages").path(0).path("id");
            return id == null || id.isMissingNode() ? "(sem id)" : id.asText();
        } catch (RestClientResponseException e) {
            // O corpo do erro da Meta explica o motivo (template não aprovado, número fora da lista de teste...).
            throw new WhatsAppSendException(
                    "HTTP " + e.getStatusCode().value() + " — " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new WhatsAppSendException("sem resposta da Cloud API: " + e.getMessage(), e);
        }
    }
}
