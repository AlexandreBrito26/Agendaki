package com.agendaki.professional;

import com.agendaki.service.ServiceOffering;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "professional")
public class Professional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToMany
    @JoinTable(
            name = "professional_service",
            joinColumns = @JoinColumn(name = "professional_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id"))
    private Set<ServiceOffering> services = new HashSet<>();

    protected Professional() {}

    public Professional(Long businessId, String name) {
        this.businessId = businessId;
        this.name = name;
    }

    public Long getId() { return id; }
    public Long getBusinessId() { return businessId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Set<ServiceOffering> getServices() { return services; }
}
