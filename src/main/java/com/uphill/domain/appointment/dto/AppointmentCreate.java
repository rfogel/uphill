package com.uphill.domain.appointment.dto;

import jakarta.validation.constraints.NotNull;

public record AppointmentCreate(@NotNull Long patientId, @NotNull Long specialtyId, @NotNull Long timeslot) {
}
