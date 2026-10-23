package com.agendaki.appointment;

import com.agendaki.business.BusinessRepository;
import com.agendaki.professional.ProfessionalRepository;
import com.agendaki.service.ServiceOfferingRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** A cada 15 min envia o lembrete dos agendamentos que começam nas próximas 24h. */
@Component
public class ReminderJob {

    private final AppointmentRepository appointments;
    private final BusinessRepository businesses;
    private final ProfessionalRepository professionals;
    private final ServiceOfferingRepository services;
    private final AppointmentMailListener mailer;

    public ReminderJob(AppointmentRepository appointments, BusinessRepository businesses,
                       ProfessionalRepository professionals, ServiceOfferingRepository services,
                       AppointmentMailListener mailer) {
        this.appointments = appointments;
        this.businesses = businesses;
        this.professionals = professionals;
        this.services = services;
        this.mailer = mailer;
    }

    @Scheduled(fixedDelayString = "${app.reminder.interval-ms:900000}", initialDelay = 60_000)
    @Transactional
    public void sendDueReminders() {
        Instant now = Instant.now();
        var due = appointments.findByStatusAndCustomerEmailIsNotNullAndReminderSentAtIsNullAndStartsAtBetween(
                AppointmentStatus.CONFIRMED, now, now.plus(Duration.ofHours(24)));

        for (Appointment a : due) {
            // Quem agendou com menos de 24h de antecedência já recebeu a confirmação: sem lembrete.
            if (a.getCreatedAt() != null && Duration.between(a.getCreatedAt(), a.getStartsAt()).toHours() < 24) {
                a.markReminderSent(now);
                continue;
            }
            var business = businesses.findById(a.getBusinessId()).orElse(null);
            var professional = professionals.findById(a.getProfessionalId()).orElse(null);
            var service = services.findById(a.getServiceId()).orElse(null);
            if (business == null || professional == null || service == null) continue;

            mailer.send(new AppointmentMailEvent(
                    AppointmentMailEvent.Kind.REMINDER, a.getCustomerEmail(), a.getCustomerName(),
                    business.getName(), business.getSlug(), service.getName(), professional.getName(),
                    a.getStartsAt(), ZoneId.of(business.getTimezone()), a.getManageToken()));
            a.markReminderSent(now);
        }
    }
}
