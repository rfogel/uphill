package com.uphill.domain.room.service;

import com.uphill.domain.room.model.Room;
import com.uphill.domain.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

    public Optional<Room> findAvailableRoomByTimeslot(Long timeslot) {
        return roomRepository.findAvailableRoomByTimeslot(timeslot);
    }
}

