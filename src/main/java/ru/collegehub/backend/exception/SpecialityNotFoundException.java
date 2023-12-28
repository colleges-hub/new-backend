package ru.collegehub.backend.exception;

public class SpecialityNotFoundException extends RuntimeException{
    public SpecialityNotFoundException(String message) {
        super(message);
    }
}
