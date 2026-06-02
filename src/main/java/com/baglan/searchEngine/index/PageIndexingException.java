package com.baglan.searchEngine.index;

public class PageIndexingException extends RuntimeException {
    public PageIndexingException(String message) {
        super(message);
    }

    public PageIndexingException(String message, Throwable cause) {
        super(message, cause);
    }
}