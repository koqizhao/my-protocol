package io.mine.protocol.server.async;

/**
 * @author koqizhao
 *
 * Oct 11, 2018
 */
public interface AsyncResponse {

    boolean isComplete();

    void complete();

    void complete(byte[] responseData);

}
