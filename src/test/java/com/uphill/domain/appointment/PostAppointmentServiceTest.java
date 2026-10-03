package com.uphill.domain.appointment;

import com.uphill.client.calendar.MedicCalendarApi;
import com.uphill.client.email.EmailApi;
import com.uphill.client.room.RoomApi;
import com.uphill.common.TestCommonUtil;
import com.uphill.common.exception.BusinessException;
import com.uphill.common.util.CommonUtil;
import com.uphill.domain.appointment.config.TopicConfig;
import com.uphill.domain.appointment.model.Appointment;
import com.uphill.domain.appointment.service.PostAppointmentService;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {"medic_reservations", "room_reservations", "email_notifications", "appointment_dead_letters"}
)
class PostAppointmentServiceTest {

    @Autowired
    private PostAppointmentService postAppointmentService;

    @Autowired
    private TopicConfig topicConfig;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @MockitoBean
    private MedicCalendarApi medicCalendarApi;

    @MockitoBean
    private RoomApi roomApi;

    @MockitoBean
    private EmailApi emailApi;

    @Test
    @DisplayName("given a valid appointment, should publish calendar, room, and email events")
    @SneakyThrows
    void test1() {
        var appointment = generateAppointment();
        postAppointmentService.setupPostAppointmentCommunication(appointment);

        verify(medicCalendarApi, timeout(5_000)).updateDoctorCalendar(any(Appointment.class));
        verify(roomApi, timeout(5_000)).reserveRoom(any(Appointment.class));
        verify(emailApi, timeout(5_000)).sendEmail(any(Appointment.class));
    }

    @Test
    @DisplayName("given a calendar integration failure, should not acknowledge the message")
    @SneakyThrows
    void test2() {
        var acknowledgment = mock(Acknowledgment.class);
        var appointment = generateAppointment();
        var payload = CommonUtil.getMapper().writeValueAsString(appointment);
        doThrow(new IllegalStateException("calendar unavailable")).when(medicCalendarApi).updateDoctorCalendar(any(Appointment.class));

        assertThrows(BusinessException.class, () -> postAppointmentService.medicReservationListener(payload, acknowledgment));

        verify(acknowledgment, never()).acknowledge();
        assertDeadLetterMessage(payload, String.valueOf(appointment.getRoom().getId()), topicConfig.getMedicReservation() + PostAppointmentService.DLQ_SUFFIX);
    }

    @Test
    @DisplayName("given a room integration failure, should not acknowledge the message")
    @SneakyThrows
    void test3() {
        var acknowledgment = mock(Acknowledgment.class);
        var appointment = generateAppointment();
        var payload = CommonUtil.getMapper().writeValueAsString(appointment);
        doThrow(new IllegalStateException("room system unavailable")).when(roomApi).reserveRoom(any(Appointment.class));

        assertThrows(BusinessException.class, () -> postAppointmentService.roomReservationListener(payload, acknowledgment));

        verify(acknowledgment, never()).acknowledge();
        assertDeadLetterMessage(payload, String.valueOf(appointment.getRoom().getId()), topicConfig.getRoomReservation() + PostAppointmentService.DLQ_SUFFIX);
    }

    @Test
    @DisplayName("given an email integration failure, should not acknowledge the message")
    @SneakyThrows
    void test4() {
        var acknowledgment = mock(Acknowledgment.class);
        var appointment = generateAppointment();
        var payload = CommonUtil.getMapper().writeValueAsString(appointment);
        doThrow(new IllegalStateException("mail system unavailable")).when(emailApi).sendEmail(any(Appointment.class));

        assertThrows(BusinessException.class, () -> postAppointmentService.emailNotificationListener(payload, acknowledgment));

        verify(acknowledgment, never()).acknowledge();
        assertDeadLetterMessage(payload, String.valueOf(appointment.getRoom().getId()), topicConfig.getEmailNotification() + PostAppointmentService.DLQ_SUFFIX);
    }

    @Test
    @DisplayName("given a Kafka broker failure, should throw a transaction failure")
    @SneakyThrows
    void test5() {
        var failedProducer = mock(KafkaTemplate.class);
        when(failedProducer.send(anyString(), anyString(), any())).thenThrow(new IllegalStateException("broker unavailable"));

        var service = new PostAppointmentService(medicCalendarApi, roomApi, emailApi, failedProducer, topicConfig);
        assertThrows(BusinessException.class, () -> service.setupPostAppointmentCommunication(generateAppointment()));
    }

    private void assertDeadLetterMessage(String expectedPayload, String key, String sourceTopic) {

        var consumerProperties = KafkaTestUtils.consumerProps(embeddedKafkaBroker, "dead-letter-test-" + sourceTopic, true);

        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        try (var consumer = new DefaultKafkaConsumerFactory<String, String>(consumerProperties).createConsumer()) {
            consumer.subscribe(List.of(sourceTopic));
            var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5));
            var deadLetterRecord = StreamSupport.stream(records.records(sourceTopic).spliterator(), false)
                    .findFirst()
                    .orElseThrow();
            assertAll(
                    () -> assertEquals(key, deadLetterRecord.key()),
                    () -> assertEquals(expectedPayload, deadLetterRecord.value())
            );
        }
    }

    private Appointment generateAppointment() {
        return TestCommonUtil.generator().nextObject(Appointment.class);
    }
}


