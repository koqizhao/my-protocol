package io.mine.protocol.data;

import io.mine.protocol.codec.DataCodec;
import io.mine.protocol.codec.FixedEncodingTransferCodec;
import io.mine.protocol.codec.LengthCodec;

/**
 * @author koqizhao
 *
 * Oct 8, 2018
 */
public class FixedEncodingProtocol extends DefaultDataProtocol {

    public FixedEncodingProtocol(byte version, DataCodec dataCodec, LengthCodec lengthCodec) {
        super(version, dataCodec, lengthCodec, new FixedEncodingTransferCodec(dataCodec, lengthCodec));
    }

}
