package com.uphill.domain.appointment.service;

import com.uphill.client.calendar.MedicCalendarApi;
import com.uphill.client.email.EmailApi;
import com.uphill.client.room.RoomApi;
import com.uphill.common.exception.BusinessException;
import com.uphill.common.exception.ErrorMessage;
import com.uphill.common.util.CommonUtil;
import com.uphill.domain.appointment.config.TopicConfig;
import com.uphill.domain.appointment.model.Appointment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostAppointmentService {

    public static final int DEFAULT_BROKER_TIMEOUT = 2;
    public static final String DLQ_SUFFIX = "_dlq";

    private final MedicCalendarApi medicCalendarApi;
    private final RoomApi roomApi;
    private final EmailApi emailApi;
    private final KafkaTemplate<String, Object> producer;
    private final TopicConfig topicConfig;

    public void setupPostAppointmentCommunication(Appointment appointment) {

        try {
            var payload = CommonUtil.getMapper().writeValueAsString(appointment);
            var medicReservationResult = producer.send(topicConfig.getMedicReservation(), String.valueOf(appointment.getRoom().getNumber()), payload);
            var roomReservationResult = producer.send(topicConfig.getRoomReservation(), String.valueOf(appointment.getRoom().getNumber()), payload);
            var emailNotificationResult = producer.send(topicConfig.getEmailNotification(), String.valueOf(appointment.getRoom().getNumber()), payload);

            CompletableFuture.allOf(medicReservationResult, roomReservationResult, emailNotificationResult)
                    .orTimeout(DEFAULT_BROKER_TIMEOUT, TimeUnit.SECONDS)
                    .thenAccept(_ -> log.debug("Successfully published appointment to Broker"))
                    .join();

        } catch (Exception e) {
            log.error("Failed to publish post appointment communication to Broker", e);
            throw new BusinessException(ErrorMessage.TRANSACTION_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @KafkaListener(topics = "${spring.kafka.topic.medic_reservation}", groupId = "${spring.kafka.consumer.group-id}", ackMode = "MANUAL")
    public void medicReservationListener(@Payload String payload, Acknowledgment acknowledgment) {

        var appointment = CommonUtil.getMapper().readValue(payload, Appointment.class);

        log.info("Received medic reservation booking: {}", appointment);

        try {
            medicCalendarApi.updateDoctorCalendar(appointment);
        } catch (Exception e) {
            log.error("Failed to process post medic reservation booking: {}", e.getMessage(), e);
            failAndSendToDeadLetterQueue(payload, String.valueOf(appointment.getRoom().getId()), topicConfig.getMedicReservation() + DLQ_SUFFIX);
        }

        acknowledgment.acknowledge();
    }

    @KafkaListener(topics = "${spring.kafka.topic.room_reservation}", groupId = "${spring.kafka.consumer.group-id}", ackMode = "MANUAL")
    public void roomReservationListener(@Payload String payload, Acknowledgment acknowledgment) {

        var appointment = CommonUtil.getMapper().readValue(payload, Appointment.class);

        log.info("Received room reservation booking: {}", appointment);

        try {
            roomApi.reserveRoom(appointment);
        } catch (Exception e) {
            log.error("Failed to process post room reservation booking: {}", e.getMessage(), e);
            failAndSendToDeadLetterQueue(payload, String.valueOf(appointment.getRoom().getId()), topicConfig.getRoomReservation() + DLQ_SUFFIX);
        }

        acknowledgment.acknowledge();
    }

    @KafkaListener(topics = "${spring.kafka.topic.email_notification}", groupId = "${spring.kafka.consumer.group-id}", ackMode = "MANUAL")
    public void emailNotificationListener(@Payload String payload, Acknowledgment acknowledgment) {

        var appointment = CommonUtil.getMapper().readValue(payload, Appointment.class);

        log.info("Received email notification booking: {}", appointment);

        try {
            emailApi.sendEmail(appointment);
        } catch (Exception e) {
            log.error("Failed to process post email notification booking: {}", e.getMessage(), e);
            failAndSendToDeadLetterQueue(payload, String.valueOf(appointment.getRoom().getId()), topicConfig.getEmailNotification() + DLQ_SUFFIX);
        }

        acknowledgment.acknowledge();
    }

    private void failAndSendToDeadLetterQueue(String payload, String key, String sourceTopic) {
        try {
            producer.send(sourceTopic, key, payload).get(DEFAULT_BROKER_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to publish {} message to dead-letter topic", sourceTopic, e);
        } catch (Exception e) {
            log.error("Failed to publish {} message to dead-letter topic", sourceTopic, e);
        }

        throw new BusinessException(ErrorMessage.TRANSACTION_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
