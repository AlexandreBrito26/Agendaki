package com.agendaki.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class WhatsAppClientTest {

    private static final String URL = "https://graph.facebook.com/v23.0/1234567890/messages";

    private MockRestServiceServer server;
    private WhatsAppClient client;

    @BeforeEach
    void setUp() {
        var props = new WhatsAppProperties(true, null, "v23.0", "1234567890", "token-secreto",
                "agendamento_confirmado", "pt_BR", "55");
        RestClient.Builder builder = RestClient.builder().baseUrl(props.baseUrl());
        server = MockRestServiceServer.bindTo(builder).build();
        client = new WhatsAppClient(props, builder.build());
    }

    @Test
    void enviaTemplateComAsVariaveisNaOrdemEDevolveOIdDaMensagem() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer token-secreto"))
                .andExpect(jsonPath("$.messaging_product").value("whatsapp"))
                .andExpect(jsonPath("$.to").value("5511999998888"))
                .andExpect(jsonPath("$.type").value("template"))
                .andExpect(jsonPath("$.template.name").value("agendamento_confirmado"))
                .andExpect(jsonPath("$.template.language.code").value("pt_BR"))
                .andExpect(jsonPath("$.template.components[0].type").value("body"))
                .andExpect(jsonPath("$.template.components[0].parameters[0].type").value("text"))
                .andExpect(jsonPath("$.template.components[0].parameters[0].text").value("Maria"))
                .andExpect(jsonPath("$.template.components[0].parameters[1].text").value("Corte"))
                .andRespond(withSuccess(
                        "{\"messaging_product\":\"whatsapp\",\"messages\":[{\"id\":\"wamid.ABC\"}]}",
                        MediaType.APPLICATION_JSON));

        String id = client.sendTemplate("5511999998888", List.of("Maria", "Corte"));

        assertThat(id).isEqualTo("wamid.ABC");
        server.verify();
    }

    @Test
    void erroDaApiViraWhatsAppSendExceptionComOMotivoENuncaComOToken() {
        server.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"message\":\"Template name does not exist\",\"code\":132001}}"));

        assertThatThrownBy(() -> client.sendTemplate("5511999998888", List.of("Maria")))
                .isInstanceOf(WhatsAppSendException.class)
                .hasMessageContaining("HTTP 400")
                .hasMessageContaining("132001")
                .hasMessageNotContaining("token-secreto");
    }
}
