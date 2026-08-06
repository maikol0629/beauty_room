package com.mr.sb.beauty_room.Controllers;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void shouldReturnStandardizedErrorPayloadForValidationErrors() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "payload");
        bindingResult.rejectValue("email", "NotBlank", "must not be blank");

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/appointment/save");
        request.setServletPath("/api/appointment/save");

        ResponseEntity<ApiErrorResponse> response = handler.handleValidationExceptions(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
        assertThat(response.getBody().getErrors()).containsKey("email");
        assertThat(response.getBody().getPath()).isEqualTo("/api/appointment/save");
    }
}
