package io.mine.protocol.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author koqizhao
 *
 * Oct 9, 2018
 */
@RunWith(Parameterized.class)
public class DataProtocolTest {

    @Parameters(name = "{index}: protocol={0}, data={1}")
    public static Collection<Object[]> data() {
        List<List<Object>> parameterValues = new ArrayList<>();
        parameterValues.add(new ArrayList<>(DataProtocols.ALL.values()));
        parameterValues.add(Arrays.asList(10, "ok", true, false, 0, 99.1F, -100.1, (byte) 33, null,
                Util.multiply("Hello, World!", 128)));
        return Util.generateParametersCombination(parameterValues);
    }

    @Parameter(0)
    public DataProtocol _protocol;

    @Parameter(1)
    public Object _data;

    @Test
    public void encodeTest() throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            _protocol.getTransferCodec().encode(os, _data);
            try (ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray())) {
                Object data = _protocol.getTransferCodec().decode(is, _data == null ? null : _data.getClass());
                Assert.assertEquals(_data, data);
            }
        }
    }

    @Test
    public void encodeTest2() {
        byte[] bytes = _protocol.getTransferCodec().encode(_data);
        Object data = _protocol.getTransferCodec().decode(bytes, _data == null ? null : _data.getClass());
        Assert.assertEquals(_data, data);
    }

}
