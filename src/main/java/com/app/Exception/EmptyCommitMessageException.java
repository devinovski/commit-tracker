package com.app.Exception;

public class EmptyCommitMessageException extends RuntimeException{
    public EmptyCommitMessageException() {
    }

    public EmptyCommitMessageException(String message) {
        super(message);
    }

    public EmptyCommitMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
