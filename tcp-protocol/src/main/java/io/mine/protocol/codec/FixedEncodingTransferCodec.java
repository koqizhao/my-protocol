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
        byte[] dataBytes = data == null ? null : getDataCodec().encode(data);
        int length = dataBytes == null ? FIN_LENGTH : dataBytes.length;
        if (length == FIN_LENGTH)
            return FIN_LENGTH_BYTES;

        byte[] lengthBytes = getLengthCodec().encode(dataBytes.length);
        byte[] bytes = newBytes(dataBytes.length + getLengthCodec().getLengthByteCount() * 2);
        System.arraycopy(lengthBytes, 0, bytes, 0, lengthBytes.length);
        System.arraycopy(dataBytes, 0, bytes, lengthBytes.length, dataBytes.length);
        System.arraycopy(FIN_LENGTH_BYTES, 0, bytes, lengthBytes.length + dataBytes.length, FIN_LENGTH_BYTES.length);
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
        if (length == FIN_LENGTH)
            return null;

        position += getLengthCodec().getLengthByteCount();
        int remainingLength = bytes.length - position;
        int expected = length + getLengthCodec().getLengthByteCount();
        if (remainingLength < expected)
            throw new InsufficientDataException(expected, remainingLength);

        return getDataCodec().decode(bytes, position, length, clazz);
    }

    @Override
    public void encode(OutputStream os, Object data) throws IOException {
        byte[] bytes = data == null ? null : getDataCodec().encode(data);
        int length = bytes == null ? FIN_LENGTH : bytes.length;
        if (length == FIN_LENGTH) {
            os.write(FIN_LENGTH_BYTES);
            return;
        }

        byte[] lengthBytes = getLengthCodec().encode(length);
        os.write(lengthBytes);
        os.write(bytes);
        os.write(FIN_LENGTH_BYTES);
    }

    @Override
    public <T> T decode(InputStream is, Class<T> clazz) throws IOException {
        byte[] lengthBytes = readBytes(is, getLengthCodec().getLengthByteCount());
        int length = getLengthCodec().decode(lengthBytes);
        if (length == FIN_LENGTH)
            return null;

        byte[] bytes = readBytes(is, length + getLengthCodec().getLengthByteCount());
        return getDataCodec().decode(bytes, 0, length, clazz);
    }

}
