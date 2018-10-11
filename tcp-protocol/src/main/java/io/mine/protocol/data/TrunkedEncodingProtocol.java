package io.mine.protocol.data;

import io.mine.protocol.codec.DataCodec;
import io.mine.protocol.codec.LengthCodec;
import io.mine.protocol.codec.TrunkedEncodingTransferCodec;

/**
 * @author koqizhao
 *
 * Oct 8, 2018
 */
public class TrunkedEncodingProtocol extends DefaultDataProtocol {

    public TrunkedEncodingProtocol(byte version, DataCodec dataCodec, LengthCodec lengthCodec, int trunkSize) {
        super(version, lengthCodec, new TrunkedEncodingTransferCodec(trunkSize, dataCodec, lengthCodec));
    }

}
