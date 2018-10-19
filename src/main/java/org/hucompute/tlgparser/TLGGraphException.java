package org.hucompute.tlgparser;

public class TLGGraphException extends Exception {

    public TLGGraphException() {
        super();
    }

    public TLGGraphException(String message) {
        super(message);
    }

    public TLGGraphException(String message, Throwable cause) {
        super(message, cause);
    }

    public TLGGraphException(Throwable cause) {
        super(cause);
    }

    protected TLGGraphException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
