package com.agendaki.appointment;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * date/startTime/endTime estão no fuso do negócio (para exibir); startsAt/endsAt são instantes UTC.
 * manageToken só vai nas respostas públicas (quem tem o link pode cancelar/remarcar).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AppointmentResponse(
        Long id,
        Long serviceId,
        String serviceName,
        Long professionalId,
        String professionalName,
        String customerName,
        String customerPhone,
        LocalDate date,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        Instant startsAt,
        Instant endsAt,
        AppointmentStatus status,
        String manageToken) {}
