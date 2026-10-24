package com.agendaki.whatsapp;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envia a confirmação por WhatsApp depois que o agendamento foi salvo (commit). Com app.whatsapp.enabled=false
 * (padrão) só registra no log, então o projeto roda sem credenciais. Falha de WhatsApp nunca derruba o agendamento.
 */
@Component
public class AppointmentWhatsAppListener {

    private static final Logger log = LoggerFactory.getLogger(AppointmentWhatsAppListener.class);
    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("EEEE, dd/MM 'às' HH:mm", Locale.forLanguageTag("pt-BR"));

    private final WhatsAppClient client;
    private final WhatsAppProperties props;
    private final String publicUrl;

    public AppointmentWhatsAppListener(WhatsAppClient client, WhatsAppProperties props,
                                       @Value("${app.public-url:http://localhost:5173}") String publicUrl) {
        this.client = client;
        this.props = props;
        this.publicUrl = publicUrl.replaceAll("/+$", "");
    }

    @Async
    @TransactionalEventListener
    public void onBooked(AppointmentWhatsAppEvent e) {
        String to = PhoneNumbers.toE164(e.phone(), props.defaultCountryCode());
        if (to == null) {
            log.info("[whatsapp] telefone {} não reconhecido, mensagem não enviada", PhoneNumbers.mask(e.phone()));
            return;
        }

        List<String> params = bodyParams(e);
        if (!props.enabled()) {
            log.info("[whatsapp desativado] para={} variáveis={}", PhoneNumbers.mask(to), params);
            return;
        }
        if (!props.hasCredentials()) {
            log.warn("[whatsapp] ativado, mas faltam WHATSAPP_PHONE_NUMBER_ID e/ou WHATSAPP_ACCESS_TOKEN");
            return;
        }

        try {
            String messageId = client.sendTemplate(to, params);
            log.info("[whatsapp] confirmação enviada para={} id={}", PhoneNumbers.mask(to), messageId);
        } catch (Exception ex) {
            log.warn("Falha ao enviar WhatsApp para {}: {}", PhoneNumbers.mask(to), ex.getMessage());
        }
    }

    /** Ordem = variáveis do template: {{1}} nome, {{2}} serviço, {{3}} profissional, {{4}} quando, {{5}} negócio, {{6}} link. */
    List<String> bodyParams(AppointmentWhatsAppEvent e) {
        String when = e.startsAt().atZone(e.zone()).format(WHEN);
        String link = publicUrl + "/" + e.slug() + "/agendamento/" + e.manageToken();
        return List.of(
                clean(e.customerName()), clean(e.serviceName()), clean(e.professionalName()),
                when, clean(e.businessName()), link);
    }

    /** A Meta recusa variável com quebra de linha, tab ou vários espaços seguidos. */
    private static String clean(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }
}
