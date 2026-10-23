package com.agendaki.service;

import com.agendaki.config.ApiException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceOfferingService {

    private final ServiceOfferingRepository repository;

    public ServiceOfferingService(ServiceOfferingRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ServiceResponse> list(Long businessId) {
        return repository.findByBusinessIdOrderByName(businessId).stream().map(ServiceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ServiceResponse get(Long businessId, Long id) {
        return ServiceResponse.from(find(businessId, id));
    }

    @Transactional
    public ServiceResponse create(Long businessId, ServiceRequest req) {
        ServiceOffering s = new ServiceOffering(
                businessId, req.name().trim(), req.durationMin(), req.priceCents());
        return ServiceResponse.from(repository.save(s));
    }

    @Transactional
    public ServiceResponse update(Long businessId, Long id, ServiceRequest req) {
        ServiceOffering s = find(businessId, id);
        s.setName(req.name().trim());
        s.setDurationMin(req.durationMin());
        s.setPriceCents(req.priceCents());
        if (req.active() != null) {
            s.setActive(req.active());
        }
        return ServiceResponse.from(s);
    }

    /** Desativa em vez de apagar, porque agendamentos antigos referenciam o serviço. */
    @Transactional
    public void deactivate(Long businessId, Long id) {
        find(businessId, id).setActive(false);
    }

    private ServiceOffering find(Long businessId, Long id) {
        return repository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> ApiException.notFound("Serviço não encontrado"));
    }
}
