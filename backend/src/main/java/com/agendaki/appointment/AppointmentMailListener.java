package com.agendaki.appointment;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envia os e-mails de confirmação/lembrete. Com app.mail.enabled=false (padrão) só registra no log,
 * então o projeto roda sem SMTP configurado. Falha de e-mail nunca derruba o agendamento.
 */
@Component
public class AppointmentMailListener {

    private static final Logger log = LoggerFactory.getLogger(AppointmentMailListener.class);
    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("EEEE, dd/MM 'às' HH:mm", Locale.forLanguageTag("pt-BR"));

    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean enabled;
    private final String from;
    private final String publicUrl;

    public AppointmentMailListener(ObjectProvider<JavaMailSender> mailSender,
                                   @Value("${app.mail.enabled:false}") boolean enabled,
                                   @Value("${app.mail.from:no-reply@agendaki.local}") String from,
                                   @Value("${app.public-url:http://localhost:5173}") String publicUrl) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
        this.publicUrl = publicUrl.replaceAll("/+$", "");
    }

    /** Dispara só depois do commit; se não houver transação (ex.: o job de lembrete), chame send(...) direto. */
    @Async
    @TransactionalEventListener
    public void onBooked(AppointmentMailEvent event) {
        send(event);
    }

    public void send(AppointmentMailEvent e) {
        if (e.to() == null || e.to().isBlank()) return;

        String when = e.startsAt().atZone(e.zone()).format(WHEN);
        String link = publicUrl + "/" + e.slug() + "/agendamento/" + e.manageToken();
        boolean reminder = e.kind() == AppointmentMailEvent.Kind.REMINDER;

        String subject = (reminder ? "Lembrete: seu horário é amanhã — " : "Agendamento confirmado — ") + e.businessName();
        String body = "Olá, " + e.customerName() + "!\n\n"
                + (reminder ? "Passando para lembrar do seu horário:\n\n" : "Seu horário está confirmado:\n\n")
                + e.serviceName() + " com " + e.professionalName() + "\n"
                + when + "\n"
                + e.businessName() + "\n\n"
                + "Precisa remarcar ou cancelar? Use este link:\n" + link + "\n";

        JavaMailSender sender = mailSender.getIfAvailable();
        if (!enabled || sender == null) {
            log.info("[e-mail desativado] para={} assunto=\"{}\"\n{}", e.to(), subject, body);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(e.to());
            msg.setSubject(subject);
            msg.setText(body);
            sender.send(msg);
        } catch (Exception ex) {
            log.warn("Falha ao enviar e-mail para {}: {}", e.to(), ex.getMessage());
        }
    }
}
