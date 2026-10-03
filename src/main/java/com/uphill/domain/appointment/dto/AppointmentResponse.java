package com.uphill.domain.appointment.dto;

public record AppointmentResponse(long appointmentId, long medicId, long roomId, long timeslotId) {
}
