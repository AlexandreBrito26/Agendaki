package com.agendaki.appointment;

import java.time.Instant;
import java.time.ZoneId;

/** Tudo que o e-mail precisa, já resolvido (o envio roda fora da transação, sem lazy loading). */
public record AppointmentMailEvent(
        Kind kind,
        String to,
        String customerName,
        String businessName,
        String slug,
        String serviceName,
        String professionalName,
        Instant startsAt,
        ZoneId zone,
        String manageToken) {

    public enum Kind { CONFIRMATION, REMINDER }
}
