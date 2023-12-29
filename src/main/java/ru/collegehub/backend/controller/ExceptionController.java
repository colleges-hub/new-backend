package ru.collegehub.backend.controller;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.collegehub.backend.api.response.admin.MessageResponse;
import ru.collegehub.backend.exception.GroupNotFoundException;
import ru.collegehub.backend.exception.RoleNotFoundException;
import ru.collegehub.backend.exception.SpecialityNotFoundException;
import ru.collegehub.backend.exception.SubjectNotFoundException;
import ru.collegehub.backend.exception.UserNotFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ExceptionController {

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<MessageResponse> handleConstraintViolationException(ConstraintViolationException e) {
        return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    private ResponseEntity<MessageResponse> userNotFound(UserNotFoundException e) {
        return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
    }

    @ExceptionHandler(GroupNotFoundException.class)
    public ResponseEntity<MessageResponse> groupNotFound(GroupNotFoundException e) {
        return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
    }

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<MessageResponse> groupNotFound(RoleNotFoundException e) {
        return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
    }

    @ExceptionHandler(SpecialityNotFoundException.class)
    public ResponseEntity<MessageResponse> specialityNotFound(SpecialityNotFoundException e) {
        return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
    }

    @ExceptionHandler(SubjectNotFoundException.class)
    public ResponseEntity<MessageResponse> subjectNotFound(SubjectNotFoundException e) {
        return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
    }
}
