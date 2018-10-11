package io.mine.protocol.async;

/**
 * @author koqizhao
 *
 * Oct 11, 2018
 */
public interface AsyncResponse {

    void complete(byte[] responseData);

}
