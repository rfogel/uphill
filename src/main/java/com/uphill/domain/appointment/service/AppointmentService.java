package com.uphill.domain.appointment.service;

import com.uphill.common.exception.BusinessException;
import com.uphill.common.exception.ErrorMessage;
import com.uphill.domain.appointment.dto.AppointmentCreate;
import com.uphill.domain.appointment.dto.AppointmentResponse;
import com.uphill.domain.appointment.model.Appointment;
import com.uphill.domain.appointment.model.AppointmentTime;
import com.uphill.domain.appointment.repository.AppointmentRepository;
import com.uphill.domain.appointment.repository.AppointmentTimeRepository;
import com.uphill.domain.medic.model.Medic;
import com.uphill.domain.medic.service.MedicService;
import com.uphill.domain.patient.model.Patient;
import com.uphill.domain.patient.service.PatientService;
import com.uphill.domain.room.model.Room;
import com.uphill.domain.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentTimeRepository appointmentTimeRepository;
    private final MedicService medicService;
    private final RoomService roomService;
    private final PatientService patientService;
    private final PostAppointmentService postAppointmentService;

    @Transactional(readOnly = true)
    public Page<Appointment> findAll(Pageable pageable) {
        return appointmentRepository.findAll(pageable);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AppointmentResponse createAppointment(AppointmentCreate appointmentCreate) {

        validateData(appointmentCreate);

        var medicAvailable = findAvailableMedic(appointmentCreate);

        var availableRoom = findAvailableRoom(appointmentCreate);

        var appointment = appointmentRepository.save(Appointment.builder()
                .medic(medicAvailable)
                .room(availableRoom)
                .appointmentTime(AppointmentTime.builder().id(appointmentCreate.timeslot()).build())
                .patient(Patient.builder().id(appointmentCreate.patientId()).build())
                .build());

        postAppointmentService.setupPostAppointmentCommunication(appointment);

        return new AppointmentResponse(appointment.getId(), appointment.getMedic().getId(), appointment.getRoom().getId(), appointment.getAppointmentTime().getId());
    }

    void validateData(AppointmentCreate appointmentCreate) {

        if (!appointmentTimeRepository.existsById(appointmentCreate.timeslot())) {
            throw new BusinessException(ErrorMessage.APPOINTMENT_TIME_NOT_FOUND, HttpStatus.BAD_REQUEST);
        }

        if (!patientService.existsById(appointmentCreate.patientId())) {
            throw new BusinessException(ErrorMessage.PATIENT_NOT_FOUND, HttpStatus.BAD_REQUEST);
        }

        appointmentRepository.findOneByPatientIdAndAppointmentTimeId(appointmentCreate.patientId(), appointmentCreate.timeslot())
                .ifPresent(_ -> {
                    throw new BusinessException(ErrorMessage.PATIENT_ALREADY_HAS_APPOINTMENT, HttpStatus.BAD_REQUEST);
                });
    }

    Medic findAvailableMedic(AppointmentCreate appointmentCreate) {

        var medicsAvailable = medicService.findBySpecialtyId(appointmentCreate.specialtyId());

        if (medicsAvailable.isEmpty()) {
            throw new BusinessException(ErrorMessage.NO_MEDICS_AVAILABLE, HttpStatus.BAD_REQUEST);
        }

        appointmentRepository.findByMedicIdInAndAppointmentTimeId(medicsAvailable.stream().map(Medic::getId).collect(Collectors.toSet()), appointmentCreate.timeslot())
                .forEach(appointment -> medicsAvailable.removeIf(medic -> medic.getId() == appointment.getMedic().getId()));

        if (medicsAvailable.isEmpty()) {
            throw new BusinessException(ErrorMessage.NO_MEDICS_AVAILABLE, HttpStatus.BAD_REQUEST);
        }

        return medicsAvailable.getFirst();
    }

    Room findAvailableRoom(AppointmentCreate appointmentCreate) {
        return roomService.findAvailableRoomByTimeslot(appointmentCreate.timeslot())
                .orElseThrow(() -> new BusinessException(ErrorMessage.NO_ROOMS_AVAILABLE, HttpStatus.BAD_REQUEST));
    }
}
