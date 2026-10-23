package com.agendaki.service;

public record ServiceResponse(Long id, String name, int durationMin, int priceCents, boolean active) {

    static ServiceResponse from(ServiceOffering s) {
        return new ServiceResponse(s.getId(), s.getName(), s.getDurationMin(), s.getPriceCents(), s.isActive());
    }
}
