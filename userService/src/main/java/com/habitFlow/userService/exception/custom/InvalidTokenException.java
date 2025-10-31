package com.habitFlow.userService.exception.custom;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) { super(message); }
}