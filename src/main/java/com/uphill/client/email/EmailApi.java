package com.uphill.client.email;

import com.uphill.domain.appointment.model.Appointment;

public interface EmailApi {
    void sendEmail(Appointment appointment) throws Exception;
}

