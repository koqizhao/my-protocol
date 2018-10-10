package io.mine.protocol.codec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * @author koqizhao
 *
 * Oct 10, 2018
 */
public class FixedEncodingTransferCodec extends AbstractTransferCodec {

    public FixedEncodingTransferCodec(DataCodec dataCodec, LengthCodec lengthCodec) {
        super(dataCodec, lengthCodec);
    }

    @Override
    public byte[] encode(Object data) {
        if (data == null)
            return getLengthCodec().encode(0);

        byte[] dataBytes = getDataCodec().encode(data);
        byte[] lengthBytes = getLengthCodec().encode(dataBytes.length);
        byte[] bytes = newBytes(dataBytes.length + lengthBytes.length);
        System.arraycopy(lengthBytes, 0, bytes, 0, lengthBytes.length);
        System.arraycopy(dataBytes, 0, bytes, lengthBytes.length, dataBytes.length);
        return bytes;
    }

    @Override
    public <T> T decode(byte[] bytes, Class<T> clazz) {
        return decode(bytes, 0, clazz);
    }

    @Override
    public <T> T decode(byte[] bytes, int position, Class<T> clazz) {
        Objects.requireNonNull(bytes, "bytes is null");

        if (position < 0 || position >= bytes.length)
            throw new ArrayIndexOutOfBoundsException(position);

        int length = getLengthCodec().decode(bytes, position);
        if (length == 0)
            return null;

        position += getLengthCodec().getLengthByteCount();
        int remainingLength = bytes.length - position;
        if (remainingLength < length)
            throw new InsufficientDataException(length, remainingLength);

        return getDataCodec().decode(bytes, position, clazz);
    }

    @Override
    public void encode(OutputStream os, Object data) throws IOException {
        byte[] bytes = data == null ? null : getDataCodec().encode(data);
        int length = bytes == null ? 0 : bytes.length;
        byte[] lengthBytes = getLengthCodec().encode(length);
        os.write(lengthBytes);
        if (length != 0)
            os.write(bytes);
    }

    @Override
    public <T> T decode(InputStream is, Class<T> clazz) throws IOException {
        byte[] lengthBytes = readBytes(is, getLengthCodec().getLengthByteCount());
        int length = getLengthCodec().decode(lengthBytes);
        if (length == 0)
            return null;

        byte[] bytes = readBytes(is, length);
        return getDataCodec().decode(bytes, clazz);
    }

}
