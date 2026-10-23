package com.agendaki.professional;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalTime;

public record WorkingHoursResponse(
        int weekday,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime) {

    static WorkingHoursResponse from(WorkingHours w) {
        return new WorkingHoursResponse(w.getWeekday(), w.getStartTime(), w.getEndTime());
    }
}
