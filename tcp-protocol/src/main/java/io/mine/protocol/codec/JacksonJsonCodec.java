package io.mine.protocol.codec;

import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author koqizhao
 *
 * Oct 9, 2018
 */
public class JacksonJsonCodec implements DataCodec {

    private ObjectMapper _objectMapper;

    public JacksonJsonCodec(ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper, "objectMapper is null");
        _objectMapper = objectMapper;
    }

    @Override
    public byte[] encode(Object obj) {
        try {
            return _objectMapper.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T decode(byte[] bytes, Class<T> clazz) {
        return decode(bytes, 0, bytes.length, clazz);
    }

    @Override
    public <T> T decode(byte[] bytes, int position, int length, Class<T> clazz) {
        Objects.requireNonNull(bytes, "bytes is null");
        try {
            return _objectMapper.readValue(bytes, position, length, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
