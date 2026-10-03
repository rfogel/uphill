package com.uphill.domain.room.repository;

import com.uphill.domain.room.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    @Query("SELECT r FROM Room r WHERE r.id NOT IN (SELECT a.room.id FROM Appointment a WHERE a.appointmentTime.id = :timeslot) limit 1")
    Optional<Room> findAvailableRoomByTimeslot(Long timeslot);
}
