package com.agendaki.service;

import jakarta.persistence.*;

/** Serviço oferecido pelo negócio (ex.: corte de cabelo). Tabela: service. */
@Entity
@Table(name = "service")
public class ServiceOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "duration_min", nullable = false)
    private int durationMin;

    @Column(name = "price_cents", nullable = false)
    private int priceCents;

    @Column(nullable = false)
    private boolean active = true;

    protected ServiceOffering() {}

    public ServiceOffering(Long businessId, String name, int durationMin, int priceCents) {
        this.businessId = businessId;
        this.name = name;
        this.durationMin = durationMin;
        this.priceCents = priceCents;
    }

    public Long getId() { return id; }
    public Long getBusinessId() { return businessId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getDurationMin() { return durationMin; }
    public void setDurationMin(int durationMin) { this.durationMin = durationMin; }
    public int getPriceCents() { return priceCents; }
    public void setPriceCents(int priceCents) { this.priceCents = priceCents; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
