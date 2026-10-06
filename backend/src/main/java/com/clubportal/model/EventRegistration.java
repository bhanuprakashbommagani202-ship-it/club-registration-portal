package com.clubportal.model;

import jakarta.persistence.*;
import java.time.Instant;

/** A student's seat at an event. One per student per event (UNIQUE constraint). */
@Entity
@Table(name = "event_registrations", uniqueConstraints = @UniqueConstraint(
        name = "uq_event_registrations_event_user", columnNames = {"event_id", "user_id"}))
public class EventRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public EventRegistration() {
    }

    public EventRegistration(Event event, User user) {
        this.event = event;
        this.user = user;
    }

    public Long getId() { return id; }
    public Event getEvent() { return event; }
    public User getUser() { return user; }
    public Instant getCreatedAt() { return createdAt; }
}
