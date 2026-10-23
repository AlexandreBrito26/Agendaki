package com.agendaki.appointment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /** Confirmados com e-mail, que começam na janela e ainda não receberam lembrete. */
    List<Appointment> findByStatusAndCustomerEmailIsNotNullAndReminderSentAtIsNullAndStartsAtBetween(
            AppointmentStatus status, Instant from, Instant to);

    Optional<Appointment> findByManageToken(String manageToken);

    List<Appointment> findByBusinessIdAndStartsAtBetweenOrderByStartsAt(
            Long businessId, Instant from, Instant to);

    /** Agendamentos confirmados do profissional que tocam o intervalo [from, to). */
    @Query("""
            select a from Appointment a
            where a.professionalId = :professionalId
              and a.status = com.agendaki.appointment.AppointmentStatus.CONFIRMED
              and a.startsAt < :to
              and a.endsAt > :from
            order by a.startsAt
            """)
    List<Appointment> findConfirmedOverlapping(
            @Param("professionalId") Long professionalId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
