package com.agendaki.whatsapp;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AppointmentWhatsAppListenerTest {

    private WhatsAppClient client;

    @BeforeEach
    void setUp() {
        client = mock(WhatsAppClient.class);
    }

    private AppointmentWhatsAppListener listener(boolean enabled, String token) {
        var props = new WhatsAppProperties(enabled, null, null, "1234567890", token, null, null, "55");
        return new AppointmentWhatsAppListener(client, props, "https://agendaki.test/");
    }

    private AppointmentWhatsAppEvent event(String phone) {
        // 17:30 UTC = 14:30 em São Paulo (segunda-feira, 26/10/2026)
        return new AppointmentWhatsAppEvent(phone, "  Maria\nSilva ", "Barbearia  Central", "central",
                "Corte", "João", Instant.parse("2026-10-26T17:30:00Z"), ZoneId.of("America/Sao_Paulo"), "tok123");
    }

    @Test
    void enviaOTemplateComAsSeisVariaveisNaOrdemCerta() {
        when(client.sendTemplate(anyString(), anyList())).thenReturn("wamid.X");

        listener(true, "token").onBooked(event("(11) 99999-8888"));

        verify(client).sendTemplate("5511999998888", List.of(
                "Maria Silva",                      // espaços e quebra de linha normalizados
                "Corte",
                "João",
                "segunda-feira, 26/10 às 14:30",
                "Barbearia Central",
                "https://agendaki.test/central/agendamento/tok123"));
    }

    @Test
    void desativadoSoRegistraNoLog() {
        listener(false, "token").onBooked(event("11999998888"));
        verifyNoInteractions(client);
    }

    @Test
    void semCredenciaisNaoChamaAApi() {
        listener(true, "").onBooked(event("11999998888"));
        verifyNoInteractions(client);
    }

    @Test
    void telefoneInvalidoNaoChamaAApi() {
        listener(true, "token").onBooked(event("12345"));
        verifyNoInteractions(client);
    }

    @Test
    void falhaDaApiNuncaPropagaParaOAgendamento() {
        when(client.sendTemplate(anyString(), anyList())).thenThrow(new WhatsAppSendException("HTTP 500"));

        assertThatCode(() -> listener(true, "token").onBooked(event("11999998888"))).doesNotThrowAnyException();
    }
}
