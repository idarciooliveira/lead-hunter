package me.iofdev.leadhunter.api;

import me.iofdev.leadhunter.input.InvalidInputException;
import me.iofdev.leadhunter.pipeline.AlreadyRunningException;
import me.iofdev.leadhunter.pipeline.BudgetExceededException;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Maps domain failures to JSON errors. Controllers throw the same
 * {@link IllegalArgumentException}s the CLI prints as {@code error: <message>};
 * a message starting with "no " means the thing was not found (404), anything
 * else is a bad request (400).
 */
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        HttpStatus status = message.startsWith("no ") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(new ApiError(message));
    }

    /** Validation failures from the same parsers the CLI uses; each broken rule is listed in {@code problems}. */
    @ExceptionHandler(InvalidInputException.class)
    ResponseEntity<ApiError> handleInvalidInput(InvalidInputException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(ex.getMessage(), ex.problems()));
    }

    @ExceptionHandler(CampaignExistsException.class)
    ResponseEntity<ApiError> handleCampaignExists(CampaignExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(ex.getMessage()));
    }

    /**
     * Budget errors reach the UI as a clear message, not a generic failure
     * (ADR 0033). The second start of a campaign returns 409 while one runs.
     */
    @ExceptionHandler(BudgetExceededException.class)
    ResponseEntity<ApiError> handleBudgetExceeded(BudgetExceededException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(ex.getMessage()));
    }

    @ExceptionHandler(AlreadyRunningException.class)
    ResponseEntity<ApiError> handleAlreadyRunning(AlreadyRunningException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(ex.getMessage()));
    }

    /** The job queue is full; the job row it opened is already failed, so a retry can start. */
    @ExceptionHandler(TaskRejectedException.class)
    ResponseEntity<ApiError> handleQueueFull(TaskRejectedException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiError("too many jobs waiting. Try again when one finishes"));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    ResponseEntity<ApiError> handleBadRequest(Exception ex) {
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
        // Jackson's message is verbose internals; the mock API and the UI only need to know the body is broken.
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError("invalid JSON body"));
    }
}
