package com.uphill.domain.medic.service;

import com.uphill.domain.medic.model.Medic;
import com.uphill.domain.medic.repository.MedicRepository;
import com.uphill.domain.specialty.model.Specialty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicService {

    private final MedicRepository medicRepository;

    public List<Medic> findBySpecialtyId(long id) {
        return medicRepository.findBySpecialtyId(id);
    }
}

