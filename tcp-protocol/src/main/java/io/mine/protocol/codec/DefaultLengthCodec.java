package io.mine.protocol.codec;

import java.util.Objects;

/**
 * @author koqizhao
 *
 * Oct 10, 2018
 */
public class DefaultLengthCodec implements LengthCodec {

    @Override
    public byte[] encode(int length) {
        if (length < 0)
            throw new IllegalArgumentException("length < 0: " + length);

        byte[] bytes = newBytes();
        bytes[0] = (byte) (length >> 24);
        bytes[1] = (byte) (length >> 16);
        bytes[2] = (byte) (length >> 8);
        bytes[3] = (byte) length;
        return bytes;
    }

    @Override
    public int getLengthByteCount() {
        return 4;
    }

    @Override
    public int decode(byte[] bytes) {
        return decode(bytes, 0);
    }

    @Override
    public int decode(byte[] bytes, int position) {
        Objects.requireNonNull(bytes, "bytes is null");

        if (position < 0 || position >= bytes.length)
            throw new ArrayIndexOutOfBoundsException(position);

        if (bytes.length - position < getLengthByteCount())
            throw new InsufficientDataException(getLengthByteCount(), bytes.length - position);

        return (bytes[position] << 24 & 0XFFFFFFFF) + (bytes[position + 1] << 16 & 0X00FFFFFF)
                + (bytes[position + 2] << 8 & 0X0000FFFF) + (bytes[position + 3] & 0X000000FF);
    }

    protected byte[] newBytes() {
        return new byte[getLengthByteCount()];
    }

}
