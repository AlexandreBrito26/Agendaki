package com.agendaki.appointment;

import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Agenda do dono do negócio (autenticado). */
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointments;

    public AppointmentController(AppointmentService appointments) {
        this.appointments = appointments;
    }

    /** Agendamentos de um dia (padrão: hoje, no fuso do negócio), com filtro opcional por profissional. */
    @GetMapping
    public List<AppointmentResponse> list(
            @AuthenticationPrincipal Long businessId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long professionalId) {
        return appointments.listForOwner(businessId, date, professionalId);
    }

    @PostMapping("/{id}/cancel")
    public AppointmentResponse cancel(@AuthenticationPrincipal Long businessId, @PathVariable Long id) {
        return appointments.cancelForOwner(businessId, id);
    }
}
