package io.mine.protocol.codec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.agrona.collections.IntArrayList;

/**
 * @author koqizhao
 *
 * Oct 10, 2018
 */
public class TrunkedEncodingTransferCodec extends AbstractTransferCodec {

    private int _trunkSize;

    protected final byte[] EMPTY_LENGTH_BYTES;
    protected final byte[] TRUNK_LENGTH_BYTES;

    public TrunkedEncodingTransferCodec(int trunkSize, DataCodec dataCodec, LengthCodec lengthCodec) {
        super(dataCodec, lengthCodec);

        if (trunkSize <= 0)
            throw new IllegalArgumentException("trunkSize <= 0");
        _trunkSize = trunkSize;

        EMPTY_LENGTH_BYTES = getLengthCodec().encode(0);
        TRUNK_LENGTH_BYTES = getLengthCodec().encode(_trunkSize);
    }

    @Override
    public byte[] encode(Object data) {
        if (data == null)
            return EMPTY_LENGTH_BYTES;

        byte[] dataBytes = getDataCodec().encode(data);
        int trunkCount = dataBytes.length / _trunkSize;
        int remaining = dataBytes.length % _trunkSize;
        boolean hasInsufficientTrunk = remaining != 0;
        if (hasInsufficientTrunk)
            trunkCount += 1;
        byte[] bytes = newBytes(dataBytes.length + (trunkCount + 1) * getLengthCodec().getLengthByteCount());

        int bytesOffset = 0;
        byte[] lengthBytes = TRUNK_LENGTH_BYTES;
        for (int i = 1, trunkSize = _trunkSize, dataBytesOffset = 0; i <= trunkCount; i++) {
            if (hasInsufficientTrunk && i == trunkCount) {
                trunkSize = remaining;
                lengthBytes = getLengthCodec().encode(remaining);
            }

            System.arraycopy(lengthBytes, 0, bytes, bytesOffset, lengthBytes.length);
            bytesOffset += lengthBytes.length;
            System.arraycopy(dataBytes, dataBytesOffset, bytes, bytesOffset, trunkSize);
            dataBytesOffset += trunkSize;
            bytesOffset += trunkSize;
        }

        System.arraycopy(EMPTY_LENGTH_BYTES, 0, bytes, bytesOffset, EMPTY_LENGTH_BYTES.length);

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

        int dataTotalSize = 0;
        int lengthTotalSize = 0;
        IntArrayList chunkSizeList = new IntArrayList();
        while (length != 0) {
            lengthTotalSize += getLengthCodec().getLengthByteCount();
            int remainingLength = bytes.length - dataTotalSize - lengthTotalSize;
            if (remainingLength < length)
                throw new InsufficientDataException(length, remainingLength);

            dataTotalSize += length;
            chunkSizeList.addInt(length);

            length = getLengthCodec().decode(bytes, position + lengthTotalSize + dataTotalSize);
        }

        byte[] dataBytes = newBytes(dataTotalSize);
        for (int i = 0, currentBytesPosition = position, currentDataBytesPosition = 0; i < chunkSizeList.size(); i++) {
            currentBytesPosition += getLengthCodec().getLengthByteCount();
            int trunkSize = chunkSizeList.getInt(i);
            System.arraycopy(bytes, currentBytesPosition, dataBytes, currentDataBytesPosition, trunkSize);
            currentBytesPosition += trunkSize;
            currentDataBytesPosition += trunkSize;
        }

        return getDataCodec().decode(dataBytes, clazz);
    }

    @Override
    public void encode(OutputStream os, Object data) throws IOException {
        if (data == null) {
            os.write(EMPTY_LENGTH_BYTES);
            os.flush();
        }

        byte[] dataBytes = getDataCodec().encode(data);
        byte[] lengthBytes = TRUNK_LENGTH_BYTES;
        for (int dataBytesOffset = 0, remaining = dataBytes.length, trunkSize = _trunkSize; remaining >= 0;) {
            if (remaining == 0) {
                os.write(EMPTY_LENGTH_BYTES);
                os.flush();
                break;
            }

            if (remaining < _trunkSize) {
                trunkSize = remaining;
                lengthBytes = getLengthCodec().encode(remaining);
            }

            os.write(lengthBytes);
            os.write(dataBytes, dataBytesOffset, trunkSize);
            os.flush();

            dataBytesOffset += trunkSize;
            remaining -= trunkSize;
        }
    }

    @Override
    public <T> T decode(InputStream is, Class<T> clazz) throws IOException {
        byte[] lengthBytes = readBytes(is, getLengthCodec().getLengthByteCount());
        int length = getLengthCodec().decode(lengthBytes);
        if (length == 0)
            return null;

        List<byte[]> trunks = new ArrayList<>();
        int dataTotalSize = 0;
        while (length != 0) {
            dataTotalSize += length;
            byte[] bytes = readBytes(is, length);
            trunks.add(bytes);

            lengthBytes = readBytes(is, getLengthCodec().getLengthByteCount());
            length = getLengthCodec().decode(lengthBytes);
        }

        byte[] dataBytes = newBytes(dataTotalSize);
        for (int i = 0, offset = 0; i < trunks.size(); i++) {
            byte[] trunk = trunks.get(i);
            System.arraycopy(trunk, 0, dataBytes, offset, trunk.length);
            offset += trunk.length;
        }

        return getDataCodec().decode(dataBytes, clazz);
    }

}
