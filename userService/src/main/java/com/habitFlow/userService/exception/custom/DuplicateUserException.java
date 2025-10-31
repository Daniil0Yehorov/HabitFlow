package com.habitFlow.userService.exception.custom;

public class DuplicateUserException extends RuntimeException {
    public DuplicateUserException(String message) { super(message); }
}