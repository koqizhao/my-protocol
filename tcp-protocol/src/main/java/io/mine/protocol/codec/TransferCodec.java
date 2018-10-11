package io.mine.protocol.codec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author koqizhao
 *
 * Oct 10, 2018
 */
public interface TransferCodec {

    int FIN_LENGTH = 0;

    byte[] encode(Object data);

    <T> T decode(byte[] bytes, Class<T> clazz);

    <T> T decode(byte[] bytes, int position, Class<T> clazz);

    void encode(OutputStream os, Object data) throws IOException;

    <T> T decode(InputStream is, Class<T> clazz) throws IOException;

}
