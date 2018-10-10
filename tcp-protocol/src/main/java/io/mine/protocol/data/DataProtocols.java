package io.mine.protocol.data;

import org.agrona.collections.Int2ObjectHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mine.protocol.codec.DefaultLengthCodec;
import io.mine.protocol.codec.JacksonJsonCodec;

/**
 * @author koqizhao
 *
 * Oct 9, 2018
 */
public class DataProtocols {

    protected DataProtocols() {

    }

    public static final DataProtocol V0 = new FixedEncodingProtocol((byte) 0, new JacksonJsonCodec(new ObjectMapper()),
            new DefaultLengthCodec()) {
        @Override
        public String toString() {
            return "JacksonJsonFixedEncoding";
        }
    };

    public static final DataProtocol V1 = new TrunkedEncodingProtocol((byte) 1,
            new JacksonJsonCodec(new ObjectMapper()), new DefaultLengthCodec(), 128) {
        @Override
        public String toString() {
            return "JacksonJsonTrunkedEncoding";
        }
    };

    public static final Int2ObjectHashMap<DataProtocol> ALL = new Int2ObjectHashMap<>();

    static {
        ALL.put(V0.getVersion(), V0);
        ALL.put(V1.getVersion(), V1);
    }

}
