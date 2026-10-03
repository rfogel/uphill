package com.uphill.client.room;

import com.uphill.domain.appointment.model.Appointment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(name = "integration.mock.enabled", havingValue = "true", matchIfMissing = true)
public class MockRoomApi implements RoomApi {

    @Override
    public void reserveRoom(Appointment appointment) {
        log.info("Mock room {} reserved for appointment {}", appointment.getRoom().getId(), appointment.getId());
    }
}

