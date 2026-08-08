package com.mr.sb.beauty_room.exceptions;

public class AppointmentConflictException extends RuntimeException {
    public AppointmentConflictException(String message) {
        super(message);
    }
}
