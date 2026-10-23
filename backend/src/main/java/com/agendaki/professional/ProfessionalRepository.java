package com.agendaki.professional;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfessionalRepository extends JpaRepository<Professional, Long> {

    List<Professional> findByBusinessIdOrderByName(Long businessId);

    List<Professional> findByBusinessIdAndActiveTrueOrderByName(Long businessId);

    Optional<Professional> findByIdAndBusinessId(Long id, Long businessId);
}
