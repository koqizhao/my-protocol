package io.mine.protocol.codec;

/**
 * @author koqizhao
 *
 * Oct 10, 2018
 */
public class InsufficientDataException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InsufficientDataException(String message) {
        super(message);
    }

    public InsufficientDataException(int expected, int actual) {
        this(String.format("data length insufficient, expected: %s, actual: %s", expected, actual));
    }

}
