package io.mine.protocol.data;

/**
 * @author koqizhao
 *
 * Oct 9, 2018
 */
public interface BytesConverter {

    byte[] toBytes(Object object);

    <T> T fromBytes(byte[] bytes, Class<T> clazz);

}
