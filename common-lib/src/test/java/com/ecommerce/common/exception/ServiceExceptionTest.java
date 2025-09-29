package com.ecommerce.common.exception;

import com.ecommerce.common.constants.ErrorCodes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for ServiceException and its factory methods.
 */
@DisplayName("ServiceException Tests")
class ServiceExceptionTest {

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create BAD_REQUEST exception")
        void testBadRequest() {
            ServiceException exception = ServiceException.badRequest("Invalid input");

            assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(exception.getMessage()).isEqualTo("Invalid input");
        }

        @Test
        @DisplayName("Should create BAD_REQUEST with error code")
        void testBadRequestWithCode() {
            ServiceException exception = ServiceException.badRequest(
                "Invalid input",
                ErrorCodes.VALIDATION_ERROR
            );

            assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(exception.getMessage()).isEqualTo("Invalid input");
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCodes.VALIDATION_ERROR);
        }

        @Test
        @DisplayName("Should create UNAUTHORIZED exception")
        void testUnauthorized() {
            ServiceException exception = ServiceException.unauthorized("Access denied");

            assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(exception.getMessage()).isEqualTo("Access denied");
        }

        @Test
        @DisplayName("Should create UNAUTHORIZED with error code")
        void testUnauthorizedWithCode() {
            ServiceException exception = ServiceException.unauthorized(
                "Invalid credentials",
                ErrorCodes.INVALID_CREDENTIALS
            );

            assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCodes.INVALID_CREDENTIALS);
        }

        @Test
        @DisplayName("Should create FORBIDDEN exception")
        void testForbidden() {
            ServiceException exception = ServiceException.forbidden("Forbidden");

            assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(exception.getMessage()).isEqualTo("Forbidden");
        }

        @Test
        @DisplayName("Should create NOT_FOUND exception")
        void testNotFound() {
            ServiceException exception = ServiceException.notFound("Resource not found");

            assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(exception.getMessage()).isEqualTo("Resource not found");
        }

        @Test
        @DisplayName("Should create NOT_FOUND with error code")
        void testNotFoundWithCode() {
            ServiceException exception = ServiceException.notFound(
                "User not found",
                ErrorCodes.USER_NOT_FOUND
            );

            assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCodes.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("Should create CONFLICT exception")
        void testConflict() {
            ServiceException exception = ServiceException.conflict("Resource already exists");

            assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(exception.getMessage()).isEqualTo("Resource already exists");
        }

        @Test
        @DisplayName("Should create CONFLICT with error code")
        void testConflictWithCode() {
            ServiceException exception = ServiceException.conflict(
                "User already exists",
                ErrorCodes.USER_ALREADY_EXISTS
            );

            assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCodes.USER_ALREADY_EXISTS);
        }

        @Test
        @DisplayName("Should create INTERNAL_SERVER_ERROR exception")
        void testInternalError() {
            ServiceException exception = ServiceException.internalError("Internal error");

            assertThat(exception.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(exception.getMessage()).isEqualTo("Internal error");
        }

        @Test
        @DisplayName("Should create INTERNAL_SERVER_ERROR with cause")
        void testInternalErrorWithCause() {
            Exception cause = new RuntimeException("Database connection failed");
            ServiceException exception = ServiceException.internalError("Operation failed", cause);

            assertThat(exception.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(exception.getMessage()).isEqualTo("Operation failed");
            assertThat(exception.getCause()).isEqualTo(cause);
        }

        @Test
        @DisplayName("Should create SERVICE_UNAVAILABLE exception")
        void testServiceUnavailable() {
            ServiceException exception = ServiceException.serviceUnavailable("Service down");

            assertThat(exception.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(exception.getMessage()).isEqualTo("Service down");
        }

        @Test
        @DisplayName("Should create GATEWAY_TIMEOUT exception")
        void testGatewayTimeout() {
            ServiceException exception = ServiceException.gatewayTimeout("Request timeout");

            assertThat(exception.getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
            assertThat(exception.getMessage()).isEqualTo("Request timeout");
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create exception with status and message")
        void testBasicConstructor() {
            ServiceException exception = new ServiceException(
                HttpStatus.BAD_REQUEST,
                "Test message"
            );

            assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(exception.getMessage()).isEqualTo("Test message");
            assertThat(exception.getErrorCode()).isNull();
        }

        @Test
        @DisplayName("Should create exception with all parameters")
        void testFullConstructor() {
            ServiceException exception = new ServiceException(
                HttpStatus.NOT_FOUND,
                "Resource not found",
                ErrorCodes.PRODUCT_NOT_FOUND,
                "12345"
            );

            assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(exception.getMessage()).isEqualTo("Resource not found");
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCodes.PRODUCT_NOT_FOUND);
            assertThat(exception.getArgs()).containsExactly("12345");
        }

        @Test
        @DisplayName("Should create exception with cause")
        void testConstructorWithCause() {
            Exception cause = new IllegalArgumentException("Invalid arg");
            ServiceException exception = new ServiceException(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                cause
            );

            assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(exception.getMessage()).isEqualTo("Validation failed");
            assertThat(exception.getCause()).isEqualTo(cause);
        }
    }

    @Nested
    @DisplayName("Getter Tests")
    class GetterTests {

        @Test
        @DisplayName("Should get status from exception")
        void testGetStatus() {
            ServiceException exception = ServiceException.notFound("Not found");

            HttpStatus status = exception.getStatus();

            assertThat(status).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(status.value()).isEqualTo(404);
        }

        @Test
        @DisplayName("Should get error code from exception")
        void testGetErrorCode() {
            ServiceException exception = ServiceException.badRequest(
                "Invalid",
                ErrorCodes.VALIDATION_ERROR
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCodes.VALIDATION_ERROR);
        }

        @Test
        @DisplayName("Should get args from exception")
        void testGetArgs() {
            ServiceException exception = new ServiceException(
                HttpStatus.BAD_REQUEST,
                "Invalid quantity: {0}",
                ErrorCodes.INVALID_QUANTITY,
                -5
            );

            assertThat(exception.getArgs()).containsExactly(-5);
        }

        @Test
        @DisplayName("Should return null for missing error code")
        void testMissingErrorCode() {
            ServiceException exception = ServiceException.notFound("Not found");

            assertThat(exception.getErrorCode()).isNull();
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should work in throw-catch scenario")
        void testThrowCatch() {
            assertThatThrownBy(() -> {
                throw ServiceException.unauthorized("Access denied", ErrorCodes.INVALID_CREDENTIALS);
            })
            .isInstanceOf(ServiceException.class)
            .hasMessage("Access denied")
            .satisfies(ex -> {
                ServiceException se = (ServiceException) ex;
                assertThat(se.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                assertThat(se.getErrorCode()).isEqualTo(ErrorCodes.INVALID_CREDENTIALS);
            });
        }

        @Test
        @DisplayName("Should preserve cause in exception chain")
        void testExceptionChain() {
            RuntimeException rootCause = new RuntimeException("Database error");
            ServiceException exception = ServiceException.internalError("Operation failed", rootCause);

            assertThat(exception.getCause()).isEqualTo(rootCause);
            assertThat(exception.getCause().getMessage()).isEqualTo("Database error");
        }

        @Test
        @DisplayName("Should support all common HTTP error codes")
        void testAllErrorCodes() {
            assertThat(ServiceException.badRequest("Bad").getStatus().value()).isEqualTo(400);
            assertThat(ServiceException.unauthorized("Unauth").getStatus().value()).isEqualTo(401);
            assertThat(ServiceException.forbidden("Forbid").getStatus().value()).isEqualTo(403);
            assertThat(ServiceException.notFound("NotFound").getStatus().value()).isEqualTo(404);
            assertThat(ServiceException.conflict("Conflict").getStatus().value()).isEqualTo(409);
            assertThat(ServiceException.internalError("Error").getStatus().value()).isEqualTo(500);
            assertThat(ServiceException.serviceUnavailable("Down").getStatus().value()).isEqualTo(503);
            assertThat(ServiceException.gatewayTimeout("Timeout").getStatus().value()).isEqualTo(504);
        }
    }
}