package com.agendaki.appointment;

import java.util.List;

/** Dados públicos para montar a página de agendamento do negócio. */
public record CatalogResponse(
        String businessName,
        String slug,
        String timezone,
        List<ServiceItem> services,
        List<ProfessionalItem> professionals) {

    public record ServiceItem(Long id, String name, int durationMin, int priceCents) {}

    public record ProfessionalItem(Long id, String name, List<Long> serviceIds) {}
}
