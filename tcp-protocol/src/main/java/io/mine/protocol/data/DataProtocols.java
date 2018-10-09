package io.mine.protocol.data;

import org.agrona.collections.Int2ObjectHashMap;

/**
 * @author koqizhao
 *
 * Oct 9, 2018
 */
public class DataProtocols {

    protected DataProtocols() {

    }

    public static final DataProtocol V0 = new FixedEncodingProtocol((byte) 0, JacksonJsonBytesConverter.DEFAULT) {
        @Override
        public String toString() {
            return "JacksonJsonFixedEncoding";
        }
    };

    public static final Int2ObjectHashMap<DataProtocol> All = new Int2ObjectHashMap<>();

    static {
        All.put(V0.getVersion(), V0);
    }

}
