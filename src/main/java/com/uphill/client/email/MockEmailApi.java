package com.uphill.client.email;

import com.uphill.domain.appointment.model.Appointment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(name = "integration.mock.enabled", havingValue = "true", matchIfMissing = true)
public class MockEmailApi implements EmailApi {

    @Override
    public void sendEmail(Appointment appointment) {
        log.info("Mock confirmation email sent to patient {} for appointment {}", appointment.getPatient().getId(), appointment.getId());
    }
}

