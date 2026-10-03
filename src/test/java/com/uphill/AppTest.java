package com.uphill;

import com.uphill.domain.appointment.service.AppointmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
public class AppTest {

    @Autowired
    private AppointmentService appointmentService;

    @Test
    public void contextLoads() {
        assertNotNull(appointmentService, "AppointmentService should be autowired");
    }
}
