package com.agendaki.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, Long> {

    List<ServiceOffering> findByBusinessIdOrderByName(Long businessId);

    List<ServiceOffering> findByBusinessIdAndActiveTrueOrderByName(Long businessId);

    Optional<ServiceOffering> findByIdAndBusinessId(Long id, Long businessId);
}
