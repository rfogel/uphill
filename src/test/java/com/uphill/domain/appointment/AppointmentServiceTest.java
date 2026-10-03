package com.uphill.domain.appointment;

import com.uphill.common.exception.BusinessException;
import com.uphill.common.exception.ErrorMessage;
import com.uphill.domain.appointment.dto.AppointmentCreate;
import com.uphill.domain.appointment.model.Appointment;
import com.uphill.domain.appointment.repository.AppointmentRepository;
import com.uphill.domain.appointment.repository.AppointmentTimeRepository;
import com.uphill.domain.appointment.service.AppointmentService;
import com.uphill.domain.appointment.service.PostAppointmentService;
import com.uphill.domain.medic.repository.MedicRepository;
import com.uphill.domain.medic.service.MedicService;
import com.uphill.domain.patient.repository.PatientRepository;
import com.uphill.domain.patient.service.PatientService;
import com.uphill.domain.room.repository.RoomRepository;
import com.uphill.domain.room.service.RoomService;
import lombok.SneakyThrows;
import org.apache.commons.lang3.IntegerRange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AppointmentServiceTest {

    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private MedicRepository medicRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private AppointmentTimeRepository appointmentTimeRepository;
    @Autowired
    private PatientRepository patientRepository;

    @BeforeAll
    void initData(@Autowired DataSource dataSource) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("sql/initial_data.sql"));
        }
    }

    void cleanup() {
        appointmentRepository.deleteAll();
    }

    AppointmentService getAppointmentService() {
        return new AppointmentService(appointmentRepository,
                appointmentTimeRepository,
                new MedicService(medicRepository),
                new RoomService(roomRepository),
                new PatientService(patientRepository),
                mock(PostAppointmentService.class));
    }

    @Nested
    @DisplayName("Tests for create()")
    class CreateTests {

        @AfterEach
        public void afterEach() {
            cleanup();
        }

        @SneakyThrows
        @Test
        @DisplayName("given some info, should create an appointment")
        void test1() {

            AppointmentService appointmentService = getAppointmentService();

            AppointmentCreate appointmentCreate = new AppointmentCreate(1L, 5L, 2L);

            var appointmentResponse = appointmentService.createAppointment(appointmentCreate);

            assertAll(
                    () -> assertNotNull(appointmentResponse),
                    () -> assertTrue(appointmentResponse.medicId() == 5L || appointmentResponse.medicId() == 10L),
                    () -> assertTrue(appointmentResponse.roomId() == 1L || appointmentResponse.roomId() == 2L),
                    () -> assertEquals(2L, appointmentResponse.timeslotId()),
                    () -> assertEquals(1, appointmentRepository.findAll().size())
            );
        }

        @SneakyThrows
        @Test
        @DisplayName("given some info, should create multiple appointments")
        void test2() {

            AppointmentService appointmentService = getAppointmentService();

            appointmentService.createAppointment(new AppointmentCreate(1L, 5L, 2L));
            var appointmentResponse = appointmentService.createAppointment(new AppointmentCreate(2L, 5L, 2L));

            assertAll(
                    () -> assertNotNull(appointmentResponse),
                    () -> assertTrue(appointmentResponse.medicId() == 5L || appointmentResponse.medicId() == 10L),
                    () -> assertTrue(appointmentResponse.roomId() == 1L || appointmentResponse.roomId() == 2L),
                    () -> assertEquals(2L, appointmentResponse.timeslotId()),
                    () -> assertEquals(2, appointmentRepository.findAll().size())
            );
        }

        @SneakyThrows
        @Test
        @DisplayName("given no medic available at the timeslot, should throw an exception")
        void test3() {

            AppointmentService appointmentService = getAppointmentService();

            appointmentService.createAppointment(new AppointmentCreate(1L, 5L, 2L));
            appointmentService.createAppointment(new AppointmentCreate(2L, 5L, 2L));
            var businessException = assertThrows(BusinessException.class, () -> appointmentService.createAppointment(new AppointmentCreate(3L, 5L, 2L)));

            assertAll(
                    () -> assertNotNull(businessException),
                    () -> assertEquals(ErrorMessage.NO_MEDICS_AVAILABLE, businessException.getMessage()),
                    () -> assertEquals(HttpStatus.BAD_REQUEST, businessException.getStatus()),
                    () -> assertEquals(2, appointmentRepository.findAll().size())
            );
        }

        @SneakyThrows
        @Test
        @DisplayName("given no room available at the timeslot, should throw an exception")
        void test4() {

            AppointmentService appointmentService = getAppointmentService();

            appointmentService.createAppointment(new AppointmentCreate(1L, 1L, 2L));
            appointmentService.createAppointment(new AppointmentCreate(2L, 2L, 2L));
            appointmentService.createAppointment(new AppointmentCreate(3L, 3L, 2L));
            appointmentService.createAppointment(new AppointmentCreate(3L, 4L, 5L));
            appointmentService.createAppointment(new AppointmentCreate(4L, 5L, 4L));
            appointmentService.createAppointment(new AppointmentCreate(4L, 4L, 2L));

            var businessException = assertThrows(BusinessException.class, () -> appointmentService.createAppointment(new AppointmentCreate(5L, 5L, 2L)));

            assertAll(
                    () -> assertNotNull(businessException),
                    () -> assertEquals(ErrorMessage.NO_ROOMS_AVAILABLE, businessException.getMessage()),
                    () -> assertEquals(HttpStatus.BAD_REQUEST, businessException.getStatus()),
                    () -> assertEquals(6, appointmentRepository.findAll().size())
            );
        }

        @SneakyThrows
        @Test
        @DisplayName("given a patient already having an appointment at the timeslot, should throw an exception")
        void test5() {

            AppointmentService appointmentService = getAppointmentService();

            appointmentService.createAppointment(new AppointmentCreate(1L, 5L, 2L));
            var businessException = assertThrows(BusinessException.class, () -> appointmentService.createAppointment(new AppointmentCreate(1L, 3L, 2L)));

            assertAll(
                    () -> assertNotNull(businessException),
                    () -> assertEquals(ErrorMessage.PATIENT_ALREADY_HAS_APPOINTMENT, businessException.getMessage()),
                    () -> assertEquals(HttpStatus.BAD_REQUEST, businessException.getStatus()),
                    () -> assertEquals(1, appointmentRepository.findAll().size())
            );
        }

        @SneakyThrows
        @Test
        @DisplayName("given no specialty available, should throw an exception")
        void test6() {

            AppointmentService appointmentService = getAppointmentService();

            var businessException = assertThrows(BusinessException.class, () -> appointmentService.createAppointment(new AppointmentCreate(1L, 15L, 1L)));

            assertAll(
                    () -> assertNotNull(businessException),
                    () -> assertEquals(ErrorMessage.NO_MEDICS_AVAILABLE, businessException.getMessage()),
                    () -> assertEquals(HttpStatus.BAD_REQUEST, businessException.getStatus()),
                    () -> assertEquals(0, appointmentRepository.findAll().size())
            );
        }

        @SneakyThrows
        @Test
        @DisplayName("given an invalid patient, should throw an exception")
        void test7() {

            AppointmentService appointmentService = getAppointmentService();

            var businessException = assertThrows(BusinessException.class, () -> appointmentService.createAppointment(new AppointmentCreate(999L, 15L, 1L)));

            assertAll(
                    () -> assertNotNull(businessException),
                    () -> assertEquals(ErrorMessage.PATIENT_NOT_FOUND, businessException.getMessage()),
                    () -> assertEquals(HttpStatus.BAD_REQUEST, businessException.getStatus()),
                    () -> assertEquals(0, appointmentRepository.findAll().size())
            );
        }

        @SneakyThrows
        @Test
        @DisplayName("given an invalid appointment time, should throw an exception")
        void test8() {

            AppointmentService appointmentService = getAppointmentService();

            var businessException = assertThrows(BusinessException.class, () -> appointmentService.createAppointment(new AppointmentCreate(1L, 15L, 999L)));

            assertAll(
                    () -> assertNotNull(businessException),
                    () -> assertEquals(ErrorMessage.APPOINTMENT_TIME_NOT_FOUND, businessException.getMessage()),
                    () -> assertEquals(HttpStatus.BAD_REQUEST, businessException.getStatus()),
                    () -> assertEquals(0, appointmentRepository.findAll().size())
            );
        }

        @SneakyThrows
        @Test
        @DisplayName("given multiple clients trying to create an appointment, should only create the available appointments and throw exceptions for the rest")
        void test9() {

            AppointmentService appointmentService = getAppointmentService();

            List<BusinessException> businessExceptions = new CopyOnWriteArrayList<>();
            List<DataAccessException> dataExceptions = new CopyOnWriteArrayList<>();
            AtomicInteger failCount = new AtomicInteger(0);

            int clientsNumber = 100;

            try (var executorService = Executors.newVirtualThreadPerTaskExecutor()) {
                IntegerRange.of(1, clientsNumber).toIntStream().forEach(i -> executorService.submit(() -> {
                    try {
                        appointmentService.createAppointment(new AppointmentCreate((long) i, 5L, 2L));
                    } catch (BusinessException e) {
                        failCount.incrementAndGet();
                        businessExceptions.add(e);
                    } catch (DataIntegrityViolationException e) {
                        failCount.incrementAndGet();
                        dataExceptions.add(e);
                    }
                }));

                executorService.shutdown();
                var termination = executorService.awaitTermination(2, TimeUnit.SECONDS);
                assertTrue(termination, "ExecutorService did not terminate in the specified time");
            }

            assertAll(
                    () -> assertEquals(2, appointmentRepository.findAll().size()),
                    () -> assertEquals(clientsNumber - 2, failCount.get()),
                    () -> assertEquals(clientsNumber - 2, businessExceptions.size() + dataExceptions.size()),
                    () -> assertTrue(businessExceptions.stream().allMatch(e -> e.getMessage().equals(ErrorMessage.NO_MEDICS_AVAILABLE)))
            );
        }
    }

    @Nested
    @DisplayName("Tests for findAll()")
    class FindAllTests {

        @BeforeEach
        void clearAppointments() {
            appointmentRepository.deleteAllInBatch();
        }

        private List<Appointment> saveAppointments(int count) {
            var appointments = List.of(
                    appointment(1L, 1L, 1L, 1L),
                    appointment(2L, 2L, 2L, 2L),
                    appointment(3L, 3L, 3L, 3L),
                    appointment(4L, 4L, 4L, 4L)
            );
            return appointmentRepository.saveAllAndFlush(appointments.subList(0, count));
        }

        private Appointment appointment(long medicId, long roomId, long appointmentTimeId, long patientId) {
            return Appointment.builder()
                    .medic(medicRepository.getReferenceById(medicId))
                    .room(roomRepository.getReferenceById(roomId))
                    .appointmentTime(appointmentTimeRepository.getReferenceById(appointmentTimeId))
                    .patient(patientRepository.getReferenceById(patientId))
                    .build();
        }

        @Test
        @DisplayName("given three appointments and first page, should return the first two appointments")
        void test1() {

            var savedAppointments = saveAppointments(3);

            var page = getAppointmentService().findAll(PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "id")));

            assertAll(
                    () -> assertEquals(3, page.getTotalElements()),
                    () -> assertEquals(2, page.getTotalPages()),
                    () -> assertEquals(2, page.getNumberOfElements()),
                    () -> assertEquals(savedAppointments.getFirst().getId(), page.getContent().getFirst().getId()),
                    () -> assertEquals(savedAppointments.get(1).getId(), page.getContent().get(1).getId()),
                    () -> assertEquals("Cardiology", page.getContent().getFirst().getMedic().getSpecialty().getName())
            );
        }

        @Test
        @DisplayName("given four appointments and second page, should return the last appointment")
        void test2() {

            var savedAppointments = saveAppointments(4);

            var page = getAppointmentService().findAll(PageRequest.of(1, 3, Sort.by(Sort.Direction.ASC, "id")));

            assertAll(
                    () -> assertEquals(4, page.getTotalElements()),
                    () -> assertEquals(2, page.getTotalPages()),
                    () -> assertEquals(1, page.getNumberOfElements()),
                    () -> assertEquals(savedAppointments.get(3).getId(), page.getContent().getFirst().getId()),
                    () -> assertEquals("Pediatrics", page.getContent().getFirst().getMedic().getSpecialty().getName())
            );
        }
    }
}

