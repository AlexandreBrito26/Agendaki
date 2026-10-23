package com.agendaki.service;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/services")
public class ServiceOfferingController {

    private final ServiceOfferingService service;

    public ServiceOfferingController(ServiceOfferingService service) {
        this.service = service;
    }

    @GetMapping
    public List<ServiceResponse> list(@AuthenticationPrincipal Long businessId) {
        return service.list(businessId);
    }

    @GetMapping("/{id}")
    public ServiceResponse get(@AuthenticationPrincipal Long businessId, @PathVariable Long id) {
        return service.get(businessId, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceResponse create(@AuthenticationPrincipal Long businessId,
                                  @Valid @RequestBody ServiceRequest request) {
        return service.create(businessId, request);
    }

    @PutMapping("/{id}")
    public ServiceResponse update(@AuthenticationPrincipal Long businessId, @PathVariable Long id,
                                  @Valid @RequestBody ServiceRequest request) {
        return service.update(businessId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@AuthenticationPrincipal Long businessId, @PathVariable Long id) {
        service.deactivate(businessId, id);
    }
}
