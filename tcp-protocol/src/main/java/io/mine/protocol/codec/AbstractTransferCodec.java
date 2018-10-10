package io.mine.protocol.codec;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * @author koqizhao
 *
 * Oct 10, 2018
 */
public abstract class AbstractTransferCodec implements TransferCodec {

    private DataCodec _dataCodec;
    private LengthCodec _lengthCodec;

    public AbstractTransferCodec(DataCodec dataCodec, LengthCodec lengthCodec) {
        Objects.requireNonNull(dataCodec, "dataCodec is null");
        Objects.requireNonNull(lengthCodec, "lengthCodec is null");

        _dataCodec = dataCodec;
        _lengthCodec = lengthCodec;
    }

    protected DataCodec getDataCodec() {
        return _dataCodec;
    }

    protected LengthCodec getLengthCodec() {
        return _lengthCodec;
    }

    protected byte[] readBytes(InputStream is, int length) throws IOException {
        byte[] bytes = new byte[length];
        for (int read = 0, total = 0; total < length && read != -1;) {
            read = is.read(bytes, total, length - total);
            if (read == -1 && total != length)
                throw new EOFException("Unexpected EOF. Total: " + total + ", Expected: " + length);

            total += read;
        }

        return bytes;
    }

    protected byte[] newBytes(int length) {
        return new byte[length];
    }

}
