package io.mine.protocol.data;

import org.junit.Assert;
import org.junit.Test;

import io.mine.protocol.codec.DefaultLengthCodec;

/**
 * @author koqizhao
 *
 * Oct 10, 2018
 */
public class LengthCodecTest {

    @Test
    public void lengthEncodeTest() {
        DefaultLengthCodec codec = new DefaultLengthCodec();
        int length = 255;
        byte[] bytes = codec.encode(length);
        int length2 = codec.decode(bytes);
        Assert.assertEquals(length, length2);
    }

}
