package com.agendaki.appointment;

import java.time.LocalDate;
import java.util.List;

/** slots: horários de início livres no fuso do negócio, no formato "HH:mm". */
public record AvailabilityResponse(LocalDate date, List<String> slots) {}
