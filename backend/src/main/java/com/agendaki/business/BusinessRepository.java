package com.agendaki.business;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRepository extends JpaRepository<Business, Long> {

    Optional<Business> findByOwnerEmail(String ownerEmail);

    Optional<Business> findBySlug(String slug);

    boolean existsByOwnerEmail(String ownerEmail);

    boolean existsBySlug(String slug);
}
