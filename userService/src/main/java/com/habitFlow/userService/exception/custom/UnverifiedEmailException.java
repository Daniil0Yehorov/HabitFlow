package com.habitFlow.userService.exception.custom;

public class UnverifiedEmailException extends RuntimeException {
    public UnverifiedEmailException(String message) { super(message); }
}
