package com.agendaki.appointment;

import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Endpoints abertos (sem login) usados pela página pública de agendamento do cliente. */
@RestController
@RequestMapping("/api/public")
public class PublicBookingController {

    private final AppointmentService appointments;

    public PublicBookingController(AppointmentService appointments) {
        this.appointments = appointments;
    }

    @GetMapping("/businesses/{slug}")
    public CatalogResponse catalog(@PathVariable String slug) {
        return appointments.catalog(slug);
    }

    @GetMapping("/businesses/{slug}/availability")
    public AvailabilityResponse availability(
            @PathVariable String slug,
            @RequestParam Long serviceId,
            @RequestParam Long professionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return appointments.availability(slug, serviceId, professionalId, date);
    }

    @PostMapping("/businesses/{slug}/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse book(@PathVariable String slug, @Valid @RequestBody BookingRequest request) {
        return appointments.book(slug, request);
    }

    @GetMapping("/appointments/{token}")
    public AppointmentResponse get(@PathVariable String token) {
        return appointments.getByToken(token);
    }

    @PostMapping("/appointments/{token}/cancel")
    public AppointmentResponse cancel(@PathVariable String token) {
        return appointments.cancelByToken(token);
    }

    @PostMapping("/appointments/{token}/reschedule")
    public AppointmentResponse reschedule(@PathVariable String token,
                                          @Valid @RequestBody RescheduleRequest request) {
        return appointments.reschedule(token, request);
    }
}
