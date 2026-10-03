package com.uphill.client.calendar;

import com.uphill.domain.appointment.model.Appointment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(name = "integration.mock.enabled", havingValue = "true", matchIfMissing = true)
public class MockMedicCalendarApi implements MedicCalendarApi {

    @Override
    public void updateDoctorCalendar(Appointment appointment) {
        log.info("Mock calendar updated for appointment {} and medic {}", appointment.getId(), appointment.getMedic().getId());
    }
}

