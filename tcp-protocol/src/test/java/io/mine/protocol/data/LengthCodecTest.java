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

    @Test
    public void byteConvertTest() {
        int data = 255;
        System.out.println(data);
        System.out.println(Integer.toBinaryString(data));
        System.out.println();

        int bytePart = (byte) 255;
        System.out.println(bytePart);
        System.out.println(Integer.toBinaryString(bytePart));
        System.out.println();
    }

}
