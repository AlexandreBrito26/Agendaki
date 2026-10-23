package com.agendaki.business;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "business")
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @Column(name = "owner_email", nullable = false, unique = true, length = 160)
    private String ownerEmail;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 50)
    private String timezone = "America/Sao_Paulo";

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected Business() {}

    public Business(String name, String slug, String ownerEmail, String passwordHash) {
        this.name = name;
        this.slug = slug;
        this.ownerEmail = ownerEmail;
        this.passwordHash = passwordHash;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public String getOwnerEmail() { return ownerEmail; }
    public String getPasswordHash() { return passwordHash; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public Instant getCreatedAt() { return createdAt; }
}
