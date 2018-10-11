package io.mine.protocol.codec;

/**
 * @author koqizhao
 *
 * Oct 9, 2018
 */
public interface DataCodec {

    byte[] encode(Object object);

    <T> T decode(byte[] bytes, Class<T> clazz);

    <T> T decode(byte[] bytes, int position, int length, Class<T> clazz);

}
