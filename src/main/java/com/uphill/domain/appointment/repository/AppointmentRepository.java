package com.uphill.domain.appointment.repository;

import com.uphill.domain.appointment.model.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @EntityGraph(value = "Appointment.detailed")
    Page<Appointment> findAll(Pageable pageable);
    List<Appointment> findByMedicIdInAndAppointmentTimeId(Set<Long> medicIds, Long appointmentTimeId);
    Optional<Appointment> findOneByPatientIdAndAppointmentTimeId(Long patientId, Long appointmentTimeId);
}
