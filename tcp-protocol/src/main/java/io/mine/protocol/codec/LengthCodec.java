package io.mine.protocol.codec;

/**
 * @author koqizhao
 *
 * Oct 10, 2018
 */
public interface LengthCodec {

    byte[] encode(int length);

    int getLengthByteCount();

    int decode(byte[] bytes);

    int decode(byte[] bytes, int position);

}
