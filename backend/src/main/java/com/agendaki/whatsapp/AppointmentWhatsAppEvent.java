package com.agendaki.whatsapp;

import java.time.Instant;
import java.time.ZoneId;

/** Tudo que a mensagem precisa, já resolvido (o envio roda fora da transação, sem lazy loading). */
public record AppointmentWhatsAppEvent(
        String phone,
        String customerName,
        String businessName,
        String slug,
        String serviceName,
        String professionalName,
        Instant startsAt,
        ZoneId zone,
        String manageToken) {}
