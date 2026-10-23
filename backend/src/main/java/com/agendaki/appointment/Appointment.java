package com.agendaki.appointment;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "appointment")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "professional_id", nullable = false)
    private Long professionalId;

    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "customer_phone", nullable = false, length = 30)
    private String customerPhone;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status = AppointmentStatus.CONFIRMED;

    @Column(name = "manage_token", nullable = false, unique = true, length = 64)
    private String manageToken;

    @Column(name = "customer_email", length = 160)
    private String customerEmail;

    @Column(name = "reminder_sent_at")
    private Instant reminderSentAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected Appointment() {}

    public Appointment(Long businessId, Long professionalId, Long serviceId,
                       String customerName, String customerPhone,
                       Instant startsAt, Instant endsAt, String manageToken) {
        this.businessId = businessId;
        this.professionalId = professionalId;
        this.serviceId = serviceId;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.manageToken = manageToken;
    }

    public void cancel() { this.status = AppointmentStatus.CANCELLED; }

    public void reschedule(Instant startsAt, Instant endsAt) {
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.reminderSentAt = null; // novo horário: o lembrete precisa ser enviado de novo
    }

    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public void markReminderSent(Instant when) { this.reminderSentAt = when; }

    public String getCustomerEmail() { return customerEmail; }
    public Instant getReminderSentAt() { return reminderSentAt; }

    public Long getId() { return id; }
    public Long getBusinessId() { return businessId; }
    public Long getProfessionalId() { return professionalId; }
    public Long getServiceId() { return serviceId; }
    public String getCustomerName() { return customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public AppointmentStatus getStatus() { return status; }
    public String getManageToken() { return manageToken; }
    public Instant getCreatedAt() { return createdAt; }
}
