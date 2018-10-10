package io.mine.protocol.codec;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author koqizhao
 *
 * Oct 10, 2018
 */
public interface StreamCodec {

    void encode(OutputStream os, Object object);

    <T> T decode(InputStream is, Class<T> clazz);

}
