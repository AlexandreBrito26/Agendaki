package com.agendaki.professional;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/professionals")
public class ProfessionalController {

    private final ProfessionalService service;

    public ProfessionalController(ProfessionalService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProfessionalResponse> list(@AuthenticationPrincipal Long businessId) {
        return service.list(businessId);
    }

    @GetMapping("/{id}")
    public ProfessionalResponse get(@AuthenticationPrincipal Long businessId, @PathVariable Long id) {
        return service.get(businessId, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfessionalResponse create(@AuthenticationPrincipal Long businessId,
                                       @Valid @RequestBody ProfessionalRequest request) {
        return service.create(businessId, request);
    }

    @PutMapping("/{id}")
    public ProfessionalResponse update(@AuthenticationPrincipal Long businessId, @PathVariable Long id,
                                       @Valid @RequestBody ProfessionalRequest request) {
        return service.update(businessId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@AuthenticationPrincipal Long businessId, @PathVariable Long id) {
        service.deactivate(businessId, id);
    }

    @GetMapping("/{id}/working-hours")
    public List<WorkingHoursResponse> getWorkingHours(@AuthenticationPrincipal Long businessId,
                                                      @PathVariable Long id) {
        return service.getWorkingHours(businessId, id);
    }

    @PutMapping("/{id}/working-hours")
    public List<WorkingHoursResponse> replaceWorkingHours(
            @AuthenticationPrincipal Long businessId, @PathVariable Long id,
            @RequestBody List<@Valid WorkingHoursRequest> request) {
        return service.replaceWorkingHours(businessId, id, request);
    }
}
