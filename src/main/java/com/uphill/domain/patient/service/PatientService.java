package com.uphill.domain.patient.service;

import com.uphill.domain.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;

    public boolean existsById(Long id) {
        return patientRepository.existsById(id);
    }
}

