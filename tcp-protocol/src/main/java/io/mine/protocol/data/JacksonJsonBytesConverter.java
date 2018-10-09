package io.mine.protocol.data;

import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author koqizhao
 *
 * Oct 9, 2018
 */
public class JacksonJsonBytesConverter implements BytesConverter {

    public static final JacksonJsonBytesConverter DEFAULT = new JacksonJsonBytesConverter(new ObjectMapper());

    private ObjectMapper _objectMapper;

    public JacksonJsonBytesConverter(ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper, "objectMapper is null");
        _objectMapper = objectMapper;
    }

    @Override
    public byte[] toBytes(Object obj) {
        try {
            return _objectMapper.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public <T> T fromBytes(byte[] bytes, Class<T> clazz) {
        try {
            return _objectMapper.readValue(bytes, clazz);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
