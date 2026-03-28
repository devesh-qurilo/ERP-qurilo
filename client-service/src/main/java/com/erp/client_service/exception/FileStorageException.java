package com.erp.client_service.exception;

public class FileStorageException extends RuntimeException {
    public FileStorageException(String msg) { super(msg); }
    public FileStorageException(String msg, Throwable cause) { super(msg, cause); }
}
