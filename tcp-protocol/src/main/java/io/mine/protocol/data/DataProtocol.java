package io.mine.protocol.data;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author koqizhao
 *
 * Oct 8, 2018
 */
public interface DataProtocol {

    byte getVersion();

    void write(OutputStream os, Object data);

    <T> T read(InputStream is, Class<T> clazz);

}
