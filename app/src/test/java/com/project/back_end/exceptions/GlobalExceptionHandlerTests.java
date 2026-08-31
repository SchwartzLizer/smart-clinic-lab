package com.project.back_end.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import jakarta.servlet.http.HttpServletRequest;

class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void domainExceptionsReturnProblemDetailsWithRequestInstance() {
        HttpServletRequest request = request("/api/doctors/99");

        var notFound = handler.handle(new ResourceNotFoundException("Doctor not found"), request).getBody();
        var conflict = handler.handle(new ResourceConflictException("Doctor already exists"), request).getBody();
        var forbidden = handler.handle(new ForbiddenOperationException("Not owner"), request).getBody();
        var unauthorized = handler.handle(new InvalidCredentialsException("Invalid credentials"), request).getBody();

        assertProblem(notFound, 404, "Not Found", "/api/doctors/99");
        assertProblem(conflict, 409, "Conflict", "/api/doctors/99");
        assertProblem(forbidden, 403, "Forbidden", "/api/doctors/99");
        assertProblem(unauthorized, 401, "Unauthorized", "/api/doctors/99");
    }

    @Test
    void validationReturnsFieldErrorsAndProblemMetadata() throws Exception {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.addError(new FieldError("request", "email", "must be a valid email"));
        Method method = GlobalExceptionHandlerTests.class.getDeclaredMethod("sample", String.class);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                new MethodParameter(method, 0), binding);

        ProblemDetail problem = handler.handle(exception, request("/api/patients")).getBody();

        assertProblem(problem, 400, "Validation failed", "/api/patients");
        assertNotNull(problem.getProperties());
        assertTrue(problem.getProperties().containsKey("errors"));
        assertEquals("must be a valid email", ((java.util.Map<?, ?>) problem.getProperties().get("errors"))
                .get("email"));
    }

    @SuppressWarnings("unused")
    private void sample(String value) {
    }

    private static HttpServletRequest request(String uri) {
        HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(uri);
        return request;
    }

    private static void assertProblem(ProblemDetail problem, int status, String title, String instance) {
        assertEquals(status, problem.getStatus());
        assertEquals(title, problem.getTitle());
        assertEquals(instance, problem.getInstance().toString());
        assertNotNull(problem.getDetail());
        assertNotNull(problem.getProperties().get("timestamp"));
    }
}
