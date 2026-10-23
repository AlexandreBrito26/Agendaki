package com.agendaki.professional;

import com.agendaki.service.ServiceOffering;
import java.util.Comparator;
import java.util.List;

public record ProfessionalResponse(
        Long id,
        String name,
        boolean active,
        List<Long> serviceIds,
        List<WorkingHoursResponse> workingHours) {

    static ProfessionalResponse from(Professional p, List<WorkingHours> hours) {
        List<Long> serviceIds = p.getServices().stream()
                .map(ServiceOffering::getId)
                .sorted(Comparator.naturalOrder())
                .toList();
        return new ProfessionalResponse(
                p.getId(), p.getName(), p.isActive(), serviceIds,
                hours.stream().map(WorkingHoursResponse::from).toList());
    }
}
