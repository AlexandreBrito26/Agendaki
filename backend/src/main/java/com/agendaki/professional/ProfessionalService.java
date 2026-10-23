package com.agendaki.professional;

import com.agendaki.config.ApiException;
import com.agendaki.service.ServiceOffering;
import com.agendaki.service.ServiceOfferingRepository;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfessionalService {

    private final ProfessionalRepository professionals;
    private final WorkingHoursRepository workingHours;
    private final ServiceOfferingRepository services;

    public ProfessionalService(ProfessionalRepository professionals,
                               WorkingHoursRepository workingHours,
                               ServiceOfferingRepository services) {
        this.professionals = professionals;
        this.workingHours = workingHours;
        this.services = services;
    }

    @Transactional(readOnly = true)
    public List<ProfessionalResponse> list(Long businessId) {
        return professionals.findByBusinessIdOrderByName(businessId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProfessionalResponse get(Long businessId, Long id) {
        return toResponse(find(businessId, id));
    }

    @Transactional
    public ProfessionalResponse create(Long businessId, ProfessionalRequest req) {
        Professional p = new Professional(businessId, req.name().trim());
        p.getServices().addAll(resolveServices(businessId, req.serviceIds()));
        return toResponse(professionals.save(p));
    }

    @Transactional
    public ProfessionalResponse update(Long businessId, Long id, ProfessionalRequest req) {
        Professional p = find(businessId, id);
        p.setName(req.name().trim());
        if (req.active() != null) {
            p.setActive(req.active());
        }
        p.getServices().clear();
        p.getServices().addAll(resolveServices(businessId, req.serviceIds()));
        return toResponse(p);
    }

    /** Desativa em vez de apagar, porque agendamentos antigos referenciam o profissional. */
    @Transactional
    public void deactivate(Long businessId, Long id) {
        find(businessId, id).setActive(false);
    }

    @Transactional(readOnly = true)
    public List<WorkingHoursResponse> getWorkingHours(Long businessId, Long id) {
        find(businessId, id);
        return workingHours.findByProfessionalIdOrderByWeekdayAscStartTimeAsc(id).stream()
                .map(WorkingHoursResponse::from)
                .toList();
    }

    /** Substitui toda a grade semanal do profissional. Lista vazia = sem expediente. */
    @Transactional
    public List<WorkingHoursResponse> replaceWorkingHours(Long businessId, Long id,
                                                          List<WorkingHoursRequest> requests) {
        find(businessId, id);
        for (WorkingHoursRequest r : requests) {
            if (r == null || r.weekday() == null || r.weekday() < 0 || r.weekday() > 6
                    || r.startTime() == null || r.endTime() == null) {
                throw ApiException.badRequest(
                        "Cada janela precisa de weekday (0 a 6), startTime e endTime (HH:mm)");
            }
        }
        List<WorkingHoursRequest> sorted = requests.stream()
                .sorted(Comparator.comparing(WorkingHoursRequest::weekday)
                        .thenComparing(WorkingHoursRequest::startTime))
                .toList();
        validateWindows(sorted);

        workingHours.deleteByProfessionalId(id);
        List<WorkingHours> saved = workingHours.saveAll(sorted.stream()
                .map(r -> new WorkingHours(id, r.weekday().shortValue(), r.startTime(), r.endTime()))
                .toList());
        return saved.stream().map(WorkingHoursResponse::from).toList();
    }

    // ---- helpers ----

    private void validateWindows(List<WorkingHoursRequest> sorted) {
        WorkingHoursRequest previous = null;
        for (WorkingHoursRequest w : sorted) {
            if (!w.endTime().isAfter(w.startTime())) {
                throw ApiException.badRequest("O horário de fim deve ser depois do início");
            }
            if (previous != null
                    && previous.weekday().equals(w.weekday())
                    && w.startTime().isBefore(previous.endTime())) {
                throw ApiException.badRequest("Há janelas de horário sobrepostas no mesmo dia");
            }
            previous = w;
        }
    }

    private Set<ServiceOffering> resolveServices(Long businessId, Set<Long> serviceIds) {
        if (serviceIds == null || serviceIds.isEmpty()) {
            return Set.of();
        }
        List<ServiceOffering> found = services.findAllById(serviceIds).stream()
                .filter(s -> s.getBusinessId().equals(businessId))
                .toList();
        if (found.size() != serviceIds.size()) {
            throw ApiException.badRequest("Um ou mais serviços informados não existem");
        }
        return new HashSet<>(found);
    }

    private Professional find(Long businessId, Long id) {
        return professionals.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> ApiException.notFound("Profissional não encontrado"));
    }

    private ProfessionalResponse toResponse(Professional p) {
        return ProfessionalResponse.from(
                p, workingHours.findByProfessionalIdOrderByWeekdayAscStartTimeAsc(p.getId()));
    }
}
