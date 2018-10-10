package io.mine.protocol.data;

import java.util.Objects;

import io.mine.protocol.codec.DataCodec;
import io.mine.protocol.codec.LengthCodec;
import io.mine.protocol.codec.TransferCodec;

/**
 * @author koqizhao
 *
 * Oct 8, 2018
 */
public class DefaultDataProtocol implements DataProtocol {

    private byte _version;
    private DataCodec _dataCodec;
    private LengthCodec _lengthCodec;
    private TransferCodec _transferCodec;

    public DefaultDataProtocol(byte version, DataCodec dataCodec, LengthCodec lengthCodec, TransferCodec transferCodec) {
        if (version < 0)
            throw new IllegalArgumentException("version < 0 : " + version);

        Objects.requireNonNull(dataCodec, "dataCodec is null");
        Objects.requireNonNull(lengthCodec, "lengthCodec is null");
        Objects.requireNonNull(transferCodec, "transferCodec is null");

        _version = version;
        _dataCodec = dataCodec;
        _lengthCodec = lengthCodec;
        _transferCodec = transferCodec;
    }

    @Override
    public byte getVersion() {
        return _version;
    }

    @Override
    public TransferCodec getTransferCodec() {
        return _transferCodec;
    }

    @Override
    public DataCodec getDataCodec() {
        return _dataCodec;
    }

    @Override
    public LengthCodec getLengthCodec() {
        return _lengthCodec;
    }

}
