package com.agendaki.professional;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

/** weekday: 0 = domingo ... 6 = sábado. Horários no formato "HH:mm". */
public record WorkingHoursRequest(
        @NotNull(message = "Informe o dia da semana")
        @Min(value = 0, message = "Dia da semana de 0 (domingo) a 6 (sábado)")
        @Max(value = 6, message = "Dia da semana de 0 (domingo) a 6 (sábado)")
        Integer weekday,

        @NotNull(message = "Informe o horário de início") LocalTime startTime,
        @NotNull(message = "Informe o horário de fim") LocalTime endTime) {}
