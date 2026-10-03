package com.uphill.domain.medic.repository;

import com.uphill.domain.medic.model.Medic;
import com.uphill.domain.specialty.model.Specialty;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicRepository extends JpaRepository<Medic, Long> {
    @EntityGraph(attributePaths = {"specialty"})
    List<Medic> findBySpecialtyId(long id);
}
