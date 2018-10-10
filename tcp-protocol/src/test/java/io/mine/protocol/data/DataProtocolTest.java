package io.mine.protocol.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

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
        return Arrays.asList(new Object[] { DataProtocols.V0, 10 }, new Object[] { DataProtocols.V0, "ok" },
                new Object[] { DataProtocols.V0, true }, new Object[] { DataProtocols.V0, false },
                new Object[] { DataProtocols.V0, 0 }, new Object[] { DataProtocols.V0, 99.1F },
                new Object[] { DataProtocols.V0, -100.1 }, new Object[] { DataProtocols.V0, (byte) 33 },
                new Object[] { DataProtocols.V0, null }, new Object[] { DataProtocols.V1, 10 },
                new Object[] { DataProtocols.V1, "ok" }, new Object[] { DataProtocols.V1, true },
                new Object[] { DataProtocols.V1, false }, new Object[] { DataProtocols.V1, 0 },
                new Object[] { DataProtocols.V1, 99.1F }, new Object[] { DataProtocols.V1, -100.1 },
                new Object[] { DataProtocols.V1, (byte) 33 }, new Object[] { DataProtocols.V1, null },
                new Object[] { DataProtocols.V1, Util.multiply("Hello, World!", 128) });
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

}
