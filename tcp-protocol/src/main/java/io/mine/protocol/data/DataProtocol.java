package io.mine.protocol.data;

import io.mine.protocol.codec.DataCodec;
import io.mine.protocol.codec.LengthCodec;
import io.mine.protocol.codec.TransferCodec;

/**
 * @author koqizhao
 *
 * Oct 8, 2018
 */
public interface DataProtocol {

    byte getVersion();

    DataCodec getDataCodec();

    LengthCodec getLengthCodec();

    TransferCodec getTransferCodec();

}
