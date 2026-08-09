package com.mr.sb.beauty_room.exceptions;

public class TenantSuspendedException extends RuntimeException {
    public TenantSuspendedException(String message) {
        super(message);
    }
}
