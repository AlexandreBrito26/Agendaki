package com.agendaki.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.agendaki.business.Business;
import com.agendaki.professional.Professional;
import com.agendaki.professional.WorkingHours;
import com.agendaki.professional.WorkingHoursRepository;
import com.agendaki.service.ServiceOffering;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AvailabilityServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private WorkingHoursRepository workingHours;
    private AppointmentRepository appointments;
    private AvailabilityService availability;

    private Business business;
    private Professional professional;
    private ServiceOffering service; // 60 minutos
    private LocalDate nextMonday;

    @BeforeEach
    void setUp() {
        workingHours = mock(WorkingHoursRepository.class);
        appointments = mock(AppointmentRepository.class);
        availability = new AvailabilityService(workingHours, appointments, 15, 60);

        business = new Business("Barbearia", "barbearia", "dono@email.com", "hash");
        professional = new Professional(1L, "Zé");
        ReflectionTestUtils.setField(professional, "id", 10L);
        service = new ServiceOffering(1L, "Corte", 60, 5000);

        nextMonday = LocalDate.now(ZONE).plusDays(1);
        while (nextMonday.getDayOfWeek() != DayOfWeek.MONDAY) {
            nextMonday = nextMonday.plusDays(1);
        }

        // Segunda-feira (weekday = 1), das 09:00 às 12:00
        when(workingHours.findByProfessionalIdAndWeekday(eq(10L), eq((short) 1)))
                .thenReturn(List.of(new WorkingHours(10L, (short) 1, LocalTime.of(9, 0), LocalTime.of(12, 0))));
        when(appointments.findConfirmedOverlapping(any(), any(), any())).thenReturn(List.of());
    }

    @Test
    void geraHorariosDePassoEmPassoAteOServicoCaberNaJanela() {
        List<LocalTime> slots = availability.freeSlots(business, professional, service, nextMonday, null);

        // Serviço de 60 min na janela 09:00-12:00 → último início possível é 11:00
        assertThat(slots).hasSize(9);
        assertThat(slots.get(0)).isEqualTo(LocalTime.of(9, 0));
        assertThat(slots.get(1)).isEqualTo(LocalTime.of(9, 15));
        assertThat(slots.get(8)).isEqualTo(LocalTime.of(11, 0));
    }

    @Test
    void removeHorariosQueSobrepoemAgendamentoExistente() {
        Instant start = nextMonday.atTime(10, 0).atZone(ZONE).toInstant();
        Instant end = nextMonday.atTime(11, 0).atZone(ZONE).toInstant();
        Appointment busy = new Appointment(1L, 10L, 1L, "Maria", "11999999999", start, end, "token");
        when(appointments.findConfirmedOverlapping(any(), any(), any())).thenReturn(List.of(busy));

        List<LocalTime> slots = availability.freeSlots(business, professional, service, nextMonday, null);

        // 09:00 termina exatamente quando o agendamento começa (livre); 11:00 começa quando ele termina (livre)
        assertThat(slots).containsExactly(LocalTime.of(9, 0), LocalTime.of(11, 0));
    }

    @Test
    void ignoraOProprioAgendamentoNaRemarcacao() {
        Instant start = nextMonday.atTime(10, 0).atZone(ZONE).toInstant();
        Instant end = nextMonday.atTime(11, 0).atZone(ZONE).toInstant();
        Appointment own = new Appointment(1L, 10L, 1L, "Maria", "11999999999", start, end, "token");
        ReflectionTestUtils.setField(own, "id", 99L);
        when(appointments.findConfirmedOverlapping(any(), any(), any())).thenReturn(List.of(own));

        List<LocalTime> slots = availability.freeSlots(business, professional, service, nextMonday, 99L);

        assertThat(slots).hasSize(9);
    }

    @Test
    void diaSemExpedienteNaoTemHorarios() {
        LocalDate tuesday = nextMonday.plusDays(1);

        assertThat(availability.freeSlots(business, professional, service, tuesday, null)).isEmpty();
    }

    @Test
    void dataNoPassadoNaoTemHorarios() {
        LocalDate yesterday = LocalDate.now(ZONE).minusDays(1);

        assertThat(availability.freeSlots(business, professional, service, yesterday, null)).isEmpty();
    }

    @Test
    void dataAlemDoLimiteNaoTemHorarios() {
        LocalDate tooFar = LocalDate.now(ZONE).plusDays(61);

        assertThat(availability.freeSlots(business, professional, service, tooFar, null)).isEmpty();
    }
}
