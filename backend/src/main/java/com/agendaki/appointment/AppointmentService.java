package com.agendaki.appointment;

import com.agendaki.business.Business;
import com.agendaki.business.BusinessRepository;
import com.agendaki.config.ApiException;
import com.agendaki.professional.Professional;
import com.agendaki.professional.ProfessionalRepository;
import com.agendaki.service.ServiceOffering;
import com.agendaki.service.ServiceOfferingRepository;
import com.agendaki.whatsapp.AppointmentWhatsAppEvent;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentService {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final String SLOT_TAKEN = "Este horário não está mais disponível";

    private final BusinessRepository businesses;
    private final ProfessionalRepository professionals;
    private final ServiceOfferingRepository services;
    private final AppointmentRepository appointments;
    private final AvailabilityService availability;
    private final ApplicationEventPublisher events;
    private final SecureRandom random = new SecureRandom();

    public AppointmentService(BusinessRepository businesses,
                              ProfessionalRepository professionals,
                              ServiceOfferingRepository services,
                              AppointmentRepository appointments,
                              AvailabilityService availability,
                              ApplicationEventPublisher events) {
        this.businesses = businesses;
        this.professionals = professionals;
        this.services = services;
        this.appointments = appointments;
        this.availability = availability;
        this.events = events;
    }

    // ------------------------------------------------------------------ público

    @Transactional(readOnly = true)
    public CatalogResponse catalog(String slug) {
        Business business = businessBySlug(slug);
        var serviceItems = services.findByBusinessIdAndActiveTrueOrderByName(business.getId()).stream()
                .map(s -> new CatalogResponse.ServiceItem(s.getId(), s.getName(), s.getDurationMin(), s.getPriceCents()))
                .toList();
        var professionalItems = professionals.findByBusinessIdAndActiveTrueOrderByName(business.getId()).stream()
                .map(p -> new CatalogResponse.ProfessionalItem(
                        p.getId(), p.getName(),
                        p.getServices().stream()
                                .filter(ServiceOffering::isActive)
                                .map(ServiceOffering::getId)
                                .sorted()
                                .toList()))
                .toList();
        return new CatalogResponse(business.getName(), business.getSlug(), business.getTimezone(),
                serviceItems, professionalItems);
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse availability(String slug, Long serviceId, Long professionalId, LocalDate date) {
        Business business = businessBySlug(slug);
        Professional professional = activeProfessional(business, professionalId);
        ServiceOffering service = activeService(business, serviceId);
        ensurePerforms(professional, service);

        List<String> slots = availability.freeSlots(business, professional, service, date, null).stream()
                .map(t -> t.format(HH_MM))
                .toList();
        return new AvailabilityResponse(date, slots);
    }

    @Transactional
    public AppointmentResponse book(String slug, BookingRequest req) {
        Business business = businessBySlug(slug);
        Professional professional = activeProfessional(business, req.professionalId());
        ServiceOffering service = activeService(business, req.serviceId());
        ensurePerforms(professional, service);

        LocalTime time = req.time().truncatedTo(ChronoUnit.MINUTES);
        List<LocalTime> free = availability.freeSlots(business, professional, service, req.date(), null);
        if (!free.contains(time)) {
            throw ApiException.conflict(SLOT_TAKEN);
        }

        ZoneId zone = ZoneId.of(business.getTimezone());
        Instant startsAt = req.date().atTime(time).atZone(zone).toInstant();
        Instant endsAt = startsAt.plus(Duration.ofMinutes(service.getDurationMin()));

        Appointment appointment = new Appointment(
                business.getId(), professional.getId(), service.getId(),
                req.customerName().trim(), req.customerPhone().trim(),
                startsAt, endsAt, newToken());
        if (req.customerEmail() != null && !req.customerEmail().isBlank()) {
            appointment.setCustomerEmail(req.customerEmail().trim());
        }
        try {
            appointment = appointments.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException e) {
            // Duas pessoas pegaram o mesmo horário ao mesmo tempo: o banco (EXCLUDE) barra a segunda.
            throw ApiException.conflict(SLOT_TAKEN);
        }
        // O e-mail só sai depois do commit (ver AppointmentMailListener).
        events.publishEvent(new AppointmentMailEvent(
                AppointmentMailEvent.Kind.CONFIRMATION, appointment.getCustomerEmail(), appointment.getCustomerName(),
                business.getName(), business.getSlug(), service.getName(), professional.getName(),
                appointment.getStartsAt(), zone, appointment.getManageToken()));
        // A confirmação por WhatsApp também só sai depois do commit (ver AppointmentWhatsAppListener).
        events.publishEvent(new AppointmentWhatsAppEvent(
                appointment.getCustomerPhone(), appointment.getCustomerName(),
                business.getName(), business.getSlug(), service.getName(), professional.getName(),
                appointment.getStartsAt(), zone, appointment.getManageToken()));
        return toResponse(appointment, zone, service.getName(), professional.getName(), true);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getByToken(String token) {
        return respondByToken(byToken(token));
    }

    @Transactional
    public AppointmentResponse cancelByToken(String token) {
        Appointment appointment = byToken(token);
        ensureCanChange(appointment);
        appointment.cancel();
        return respondByToken(appointment);
    }

    @Transactional
    public AppointmentResponse reschedule(String token, RescheduleRequest req) {
        Appointment appointment = byToken(token);
        ensureCanChange(appointment);

        Business business = businesses.findById(appointment.getBusinessId())
                .orElseThrow(() -> ApiException.notFound("Negócio não encontrado"));
        Professional professional = professionals.findById(appointment.getProfessionalId())
                .orElseThrow(() -> ApiException.notFound("Profissional não encontrado"));
        ServiceOffering service = services.findById(appointment.getServiceId())
                .orElseThrow(() -> ApiException.notFound("Serviço não encontrado"));
        if (!professional.isActive() || !service.isActive()) {
            throw ApiException.conflict("Este serviço ou profissional não está mais disponível. Entre em contato com o negócio");
        }

        LocalTime time = req.time().truncatedTo(ChronoUnit.MINUTES);
        List<LocalTime> free = availability.freeSlots(business, professional, service, req.date(), appointment.getId());
        if (!free.contains(time)) {
            throw ApiException.conflict(SLOT_TAKEN);
        }

        ZoneId zone = ZoneId.of(business.getTimezone());
        Instant startsAt = req.date().atTime(time).atZone(zone).toInstant();
        appointment.reschedule(startsAt, startsAt.plus(Duration.ofMinutes(service.getDurationMin())));
        try {
            appointments.flush();
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflict(SLOT_TAKEN);
        }
        return toResponse(appointment, zone, service.getName(), professional.getName(), true);
    }

    // ------------------------------------------------------------------ dono

    @Transactional(readOnly = true)
    public List<AppointmentResponse> listForOwner(Long businessId, LocalDate date, Long professionalId) {
        Business business = businesses.findById(businessId)
                .orElseThrow(() -> ApiException.notFound("Negócio não encontrado"));
        ZoneId zone = ZoneId.of(business.getTimezone());
        LocalDate day = date != null ? date : LocalDate.now(zone);

        Instant from = day.atStartOfDay(zone).toInstant();
        Instant to = day.plusDays(1).atStartOfDay(zone).toInstant().minusSeconds(1);
        List<Appointment> list = appointments
                .findByBusinessIdAndStartsAtBetweenOrderByStartsAt(businessId, from, to).stream()
                .filter(a -> professionalId == null || a.getProfessionalId().equals(professionalId))
                .toList();

        Map<Long, String> serviceNames = services.findAllById(
                        list.stream().map(Appointment::getServiceId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(ServiceOffering::getId, ServiceOffering::getName));
        Map<Long, String> professionalNames = professionals.findAllById(
                        list.stream().map(Appointment::getProfessionalId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Professional::getId, Professional::getName));

        return list.stream()
                .map(a -> toResponse(a, zone,
                        serviceNames.getOrDefault(a.getServiceId(), "—"),
                        professionalNames.getOrDefault(a.getProfessionalId(), "—"),
                        false))
                .toList();
    }

    @Transactional
    public AppointmentResponse cancelForOwner(Long businessId, Long id) {
        Appointment appointment = appointments.findById(id)
                .filter(a -> a.getBusinessId().equals(businessId))
                .orElseThrow(() -> ApiException.notFound("Agendamento não encontrado"));
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw ApiException.conflict("Este agendamento já foi cancelado");
        }
        appointment.cancel();

        Business business = businesses.findById(businessId).orElseThrow();
        return toResponse(appointment, ZoneId.of(business.getTimezone()),
                serviceName(appointment.getServiceId()), professionalName(appointment.getProfessionalId()), false);
    }

    // ------------------------------------------------------------------ helpers

    private Business businessBySlug(String slug) {
        return businesses.findBySlug(slug)
                .orElseThrow(() -> ApiException.notFound("Negócio não encontrado"));
    }

    private Professional activeProfessional(Business business, Long id) {
        return professionals.findByIdAndBusinessId(id, business.getId())
                .filter(Professional::isActive)
                .orElseThrow(() -> ApiException.notFound("Profissional não encontrado"));
    }

    private ServiceOffering activeService(Business business, Long id) {
        return services.findByIdAndBusinessId(id, business.getId())
                .filter(ServiceOffering::isActive)
                .orElseThrow(() -> ApiException.notFound("Serviço não encontrado"));
    }

    private void ensurePerforms(Professional professional, ServiceOffering service) {
        boolean performs = professional.getServices().stream()
                .anyMatch(s -> s.getId().equals(service.getId()));
        if (!performs) {
            throw ApiException.badRequest("Este profissional não realiza este serviço");
        }
    }

    private Appointment byToken(String token) {
        return appointments.findByManageToken(token)
                .orElseThrow(() -> ApiException.notFound("Agendamento não encontrado"));
    }

    private void ensureCanChange(Appointment appointment) {
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw ApiException.conflict("Este agendamento já foi cancelado");
        }
        if (!appointment.getStartsAt().isAfter(Instant.now())) {
            throw ApiException.conflict("Este agendamento já passou e não pode ser alterado");
        }
    }

    private AppointmentResponse respondByToken(Appointment appointment) {
        Business business = businesses.findById(appointment.getBusinessId()).orElseThrow();
        return toResponse(appointment, ZoneId.of(business.getTimezone()),
                serviceName(appointment.getServiceId()), professionalName(appointment.getProfessionalId()), true);
    }

    private String serviceName(Long id) {
        return services.findById(id).map(ServiceOffering::getName).orElse("—");
    }

    private String professionalName(Long id) {
        return professionals.findById(id).map(Professional::getName).orElse("—");
    }

    private String newToken() {
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); // 32 caracteres
    }

    private AppointmentResponse toResponse(Appointment a, ZoneId zone, String serviceName,
                                           String professionalName, boolean withToken) {
        var start = a.getStartsAt().atZone(zone);
        var end = a.getEndsAt().atZone(zone);
        return new AppointmentResponse(
                a.getId(), a.getServiceId(), serviceName, a.getProfessionalId(), professionalName,
                a.getCustomerName(), a.getCustomerPhone(),
                start.toLocalDate(), start.toLocalTime(), end.toLocalTime(),
                a.getStartsAt(), a.getEndsAt(), a.getStatus(),
                withToken ? a.getManageToken() : null);
    }
}
