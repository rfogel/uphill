package com.uphill.domain.appointment.repository;

import com.uphill.domain.appointment.model.AppointmentTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentTimeRepository extends JpaRepository<AppointmentTime, Long> {
}
