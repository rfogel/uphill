package com.uphill.client.calendar;

import com.uphill.domain.appointment.model.Appointment;

public interface MedicCalendarApi {
    void updateDoctorCalendar(Appointment appointment) throws Exception;
}

