package io.mine.protocol.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * @author koqizhao
 *
 * Oct 8, 2018
 */
public class FixedEncodingProtocol implements DataProtocol {

    private byte _version;
    private BytesConverter _converter;

    public FixedEncodingProtocol(byte version, BytesConverter converter) {
        if (version < 0)
            throw new IllegalArgumentException("version < 0 : " + version);

        Objects.requireNonNull(converter, "converter is null");

        _version = version;
        _converter = converter;
    }

    @Override
    public byte getVersion() {
        return _version;
    }

    @Override
    public void write(OutputStream os, Object data) {
        byte[] bytes = data == null ? null : _converter.toBytes(data);
        int length = bytes == null ? 0 : bytes.length;
        writeLength(os, length);

        if (length == 0)
            return;

        try {
            os.write(bytes);
        } catch (IOException e) {
            throw new DataProtocolException(e);
        }
    }

    @Override
    public <T> T read(InputStream is, Class<T> clazz) {
        int length = readLength(is);
        if (length == 0)
            return null;

        byte[] bytes = readBytes(is, length);
        return _converter.fromBytes(bytes, clazz);
    }

    protected void writeLength(OutputStream os, int length) {
        if (length < 0)
            throw new IllegalArgumentException("length < 0: " + length);

        byte[] bytes = new byte[] { (byte) (length >> 24), (byte) (length >> 16), (byte) (length >> 8), (byte) length };
        try {
            os.write(bytes);
        } catch (IOException e) {
            throw new DataProtocolException(e);
        }
    }

    protected int readLength(InputStream is) {
        byte[] bytes = readBytes(is, 4);
        return (bytes[0] << 24) + (bytes[1] << 16) + (bytes[2] << 8) + bytes[3];
    }

    protected byte[] readBytes(InputStream is, int length) {
        byte[] bytes = new byte[length];
        try {
            for (int read = 0, total = 0; total < length && read != -1;) {
                read = is.read(bytes, total, length - total);
                if (read == -1 && total != length)
                    throw new DataProtocolException("Unexcepted EOF. Total: " + total + ", Expected: " + length);

                total += read;
            }

            return bytes;
        } catch (IOException e) {
            throw new DataProtocolException(e);
        }
    }

}
