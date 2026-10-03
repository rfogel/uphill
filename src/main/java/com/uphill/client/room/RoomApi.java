package com.uphill.client.room;

import com.uphill.domain.appointment.model.Appointment;

public interface RoomApi {
    void reserveRoom(Appointment appointment) throws Exception;
}

