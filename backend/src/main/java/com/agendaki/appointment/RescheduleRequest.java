package com.agendaki.appointment;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record RescheduleRequest(
        @NotNull(message = "Escolha o dia") LocalDate date,
        @NotNull(message = "Escolha o horário") LocalTime time) {}
