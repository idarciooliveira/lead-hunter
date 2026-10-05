package me.iofdev.leadhunter.pipeline;

public class AlreadyRunningException extends RuntimeException {

    public AlreadyRunningException(String message) {
        super(message);
    }
}
