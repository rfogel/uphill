package com.uphill.common.exception;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class ErrorMessage {

    public static final String INVALID_VALUES = "Invalid values";
    public static final String NO_MEDICS_AVAILABLE = "No medics available for the selected specialty and timeslot";
    public static final String NO_ROOMS_AVAILABLE = "No rooms available for the selected timeslot";
    public static final String PATIENT_ALREADY_HAS_APPOINTMENT = "Patient already has an appointment at this timeslot";
    public static final String APPOINTMENT_TIME_NOT_FOUND = "Appointment time not found";
    public static final String PATIENT_NOT_FOUND = "Patient not found";
    public static final String TRANSACTION_FAILED = "Transaction could not be completed at this time";
}
