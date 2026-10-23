package com.agendaki.appointment;

import com.agendaki.business.Business;
import com.agendaki.professional.Professional;
import com.agendaki.professional.WorkingHours;
import com.agendaki.professional.WorkingHoursRepository;
import com.agendaki.service.ServiceOffering;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Horário livre = janela de trabalho do profissional naquele dia da semana
 * − agendamentos confirmados − horários no passado, respeitando a duração do serviço.
 */
@Service
public class AvailabilityService {

    private final WorkingHoursRepository workingHours;
    private final AppointmentRepository appointments;
    private final int stepMinutes;
    private final int maxDaysAhead;

    public AvailabilityService(
            WorkingHoursRepository workingHours,
            AppointmentRepository appointments,
            @Value("${app.booking.slot-step-minutes:15}") int stepMinutes,
            @Value("${app.booking.max-days-ahead:60}") int maxDaysAhead) {
        this.workingHours = workingHours;
        this.appointments = appointments;
        this.stepMinutes = stepMinutes;
        this.maxDaysAhead = maxDaysAhead;
    }

    /**
     * @param excludeAppointmentId agendamento a ignorar na checagem de conflito (usado na remarcação,
     *                             para o cliente poder escolher um horário que se sobrepõe ao atual); pode ser null
     */
    public List<LocalTime> freeSlots(Business business, Professional professional, ServiceOffering service,
                                     LocalDate date, Long excludeAppointmentId) {
        ZoneId zone = ZoneId.of(business.getTimezone());
        LocalDate today = LocalDate.now(zone);
        if (date.isBefore(today) || date.isAfter(today.plusDays(maxDaysAhead))) {
            return List.of();
        }

        // Java: segunda = 1 ... domingo = 7. No banco: domingo = 0 ... sábado = 6.
        short weekday = (short) (date.getDayOfWeek().getValue() % 7);
        List<WorkingHours> windows = workingHours.findByProfessionalIdAndWeekday(professional.getId(), weekday)
                .stream()
                .sorted(Comparator.comparing(WorkingHours::getStartTime))
                .toList();
        if (windows.isEmpty()) {
            return List.of();
        }

        Instant dayStart = date.atStartOfDay(zone).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();
        List<Appointment> busy = appointments
                .findConfirmedOverlapping(professional.getId(), dayStart, dayEnd).stream()
                .filter(a -> excludeAppointmentId == null || !a.getId().equals(excludeAppointmentId))
                .toList();

        Instant now = Instant.now();
        Duration duration = Duration.ofMinutes(service.getDurationMin());
        List<LocalTime> slots = new ArrayList<>();

        for (WorkingHours window : windows) {
            int startMin = window.getStartTime().toSecondOfDay() / 60;
            int endMin = window.getEndTime().toSecondOfDay() / 60;
            for (int m = startMin; m + service.getDurationMin() <= endMin; m += stepMinutes) {
                LocalTime time = LocalTime.ofSecondOfDay(m * 60L);
                Instant slotStart = date.atTime(time).atZone(zone).toInstant();
                Instant slotEnd = slotStart.plus(duration);
                if (!slotStart.isAfter(now)) {
                    continue;
                }
                boolean conflict = busy.stream()
                        .anyMatch(a -> slotStart.isBefore(a.getEndsAt()) && slotEnd.isAfter(a.getStartsAt()));
                if (!conflict) {
                    slots.add(time);
                }
            }
        }
        return slots;
    }
}
