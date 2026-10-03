package com.uphill.domain.appointment;

import com.uphill.common.TestCommonUtil;
import com.uphill.common.exception.BusinessException;
import com.uphill.common.exception.ErrorMessage;
import com.uphill.common.exception.GlobalExceptionHandler;
import com.uphill.common.util.CommonUtil;
import com.uphill.domain.appointment.controller.AppointmentController;
import com.uphill.domain.appointment.dto.AppointmentCreate;
import com.uphill.domain.appointment.dto.AppointmentResponse;
import com.uphill.domain.appointment.service.AppointmentService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WebMvcTest(value = {AppointmentController.class, GlobalExceptionHandler.class})
public class AppointmentControllerTest {

    private ObjectMapper parser;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AppointmentService appointmentService;

    @BeforeAll
    void setup() {
        parser = CommonUtil.getMapper();
    }

    @Nested
    @DisplayName("Tests for create()")
    class CreateTests {

        @SneakyThrows
        @Test
        @DisplayName("given a appointment, should return 201")
        void test1() {

            var request = new AppointmentCreate(1L, 1L, 1L);
            var response = new AppointmentResponse(1L, 1L, 1L, 1L);

            doReturn(response).when(appointmentService).createAppointment(any());

            MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/appointments")
                            .contentType(APPLICATION_JSON)
                            .content(parser.writeValueAsString(request)))
                    .andExpectAll(status().isCreated()).andReturn();

            AppointmentResponse apiResponse = parser.readValue(result.getResponse().getContentAsString(), AppointmentResponse.class);

            assertAll(
                    () -> assertEquals(1L, apiResponse.appointmentId()),
                    () -> assertEquals(1L, apiResponse.timeslotId()),
                    () -> assertEquals(1L, apiResponse.medicId()),
                    () -> assertEquals(1L, apiResponse.roomId())
            );
        }

        @SneakyThrows
        @Test
        @DisplayName("given an invalid request, should return 400")
        void test2() {

            var request = new AppointmentCreate(1L, 1L, 1L);

            doThrow(new BusinessException("something went wrong", HttpStatus.BAD_REQUEST)).when(appointmentService).createAppointment(any());

            MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/appointments")
                            .contentType(APPLICATION_JSON)
                            .content(parser.writeValueAsString(request)))
                    .andExpectAll(status().isBadRequest()).andReturn();

            ProblemDetail apiResponse = parser.readValue(result.getResponse().getContentAsString(), ProblemDetail.class);

            assertAll(
                    () -> assertEquals("something went wrong", apiResponse.getDetail()),
                    () -> assertEquals(400, apiResponse.getStatus())
            );
        }

        @SneakyThrows
        @Test
        @DisplayName("given a failed transaction, should return 409")
        void test3() {

            var request = new AppointmentCreate(1L, 1L, 1L);

            doThrow(new DataIntegrityViolationException("something went wrong")).when(appointmentService).createAppointment(any());

            MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/appointments")
                            .contentType(APPLICATION_JSON)
                            .content(parser.writeValueAsString(request)))
                    .andExpectAll(status().isConflict()).andReturn();

            ProblemDetail apiResponse = parser.readValue(result.getResponse().getContentAsString(), ProblemDetail.class);

            assertAll(
                    () -> assertEquals(ErrorMessage.TRANSACTION_FAILED, apiResponse.getDetail()),
                    () -> assertEquals(409, apiResponse.getStatus())
            );
        }

        @SneakyThrows
        @Test
        @DisplayName("given a incorrect request, should return 400")
        void test4() {

            var request = new AppointmentCreate(1L, 1L, null);

            MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/appointments")
                            .contentType(APPLICATION_JSON)
                            .content(parser.writeValueAsString(request)))
                    .andExpectAll(status().isBadRequest()).andReturn();

            ProblemDetail apiResponse = parser.readValue(result.getResponse().getContentAsString(), ProblemDetail.class);

            assertAll(
                    () -> assertEquals("appointmentCreate.timeslot: must not be null", apiResponse.getDetail()),
                    () -> assertEquals(400, apiResponse.getStatus())
            );
        }
    }

    @Nested
    @DisplayName("Tests for findAll()")
    class FindAllTests {

        @SneakyThrows
        @Test
        @DisplayName("given offset and limit, should return a paginated list")
        void test1() {

            var response = new AppointmentResponse(1L, 2L, 3L, 4L);

            doReturn(new PageImpl<>(List.of(response), PageRequest.of(1, 2), 5)).when(appointmentService).findAll(any());

            mvc.perform(MockMvcRequestBuilders.get("/appointments")
                            .param("offset", "2")
                            .param("limit", "2"))
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.content[0].appointmentId").value(1),
                            jsonPath("$.totalElements").value(5),
                            jsonPath("$.size").value(2)
                    );
        }

        @SneakyThrows
        @Test
        @DisplayName("given an invalid limit, should return 400")
        void test2() {
            mvc.perform(MockMvcRequestBuilders.get("/appointments").param("limit", "0")).andExpect(status().isBadRequest());
        }
    }
}
