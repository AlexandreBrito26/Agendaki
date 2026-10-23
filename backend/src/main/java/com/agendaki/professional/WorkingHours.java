package com.agendaki.professional;

import jakarta.persistence.*;
import java.time.LocalTime;

/** Janela de trabalho semanal. weekday: 0 = domingo ... 6 = sábado. */
@Entity
@Table(name = "working_hours")
public class WorkingHours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "professional_id", nullable = false)
    private Long professionalId;

    @Column(nullable = false)
    private short weekday;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    protected WorkingHours() {}

    public WorkingHours(Long professionalId, short weekday, LocalTime startTime, LocalTime endTime) {
        this.professionalId = professionalId;
        this.weekday = weekday;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Long getId() { return id; }
    public Long getProfessionalId() { return professionalId; }
    public short getWeekday() { return weekday; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
}
