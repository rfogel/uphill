package com.uphill.domain.appointment.controller;

import com.uphill.common.component.OffsetBasedPageRequest;
import com.uphill.domain.appointment.dto.AppointmentCreate;
import com.uphill.domain.appointment.dto.AppointmentResponse;
import com.uphill.domain.appointment.model.Appointment;
import com.uphill.domain.appointment.service.AppointmentService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
@Validated
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping
    public Page<Appointment> findAll(@RequestParam(defaultValue = "0") @Min(0) long offset, @RequestParam(defaultValue = "20") @Min(1) int limit) {
        var pageable = new OffsetBasedPageRequest(offset, limit, Sort.Direction.ASC, "id");
        return appointmentService.findAll(pageable);
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> create(@RequestBody @Validated AppointmentCreate appointmentCreate) {
        AppointmentResponse appointmentResponse = appointmentService.createAppointment(appointmentCreate);
        return ResponseEntity.created(URI.create("/appointments")).body(appointmentResponse);
    }
}
