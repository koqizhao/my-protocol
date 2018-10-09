package io.mine.protocol.data;

/**
 * @author koqizhao
 *
 * Oct 8, 2018
 */
public class DataProtocolException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DataProtocolException() {
        super();
    }

    public DataProtocolException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public DataProtocolException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataProtocolException(String message) {
        super(message);
    }

    public DataProtocolException(Throwable cause) {
        super(cause);
    }

}
