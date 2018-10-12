package io.mine.protocol.async;

import java.util.List;

import io.mine.protocol.codec.LengthCodec;
import io.mine.protocol.codec.TransferCodec;
import io.mine.protocol.data.DataProtocol;
import io.mine.protocol.data.DataProtocolException;
import io.mine.protocol.data.DataProtocols;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * @author koqizhao
 *
 * Oct 12, 2018
 */
class NettyDecoder<T> extends ByteToMessageDecoder {

    private Class<T> _dataType;
    private volatile DataProtocol _dataProtocol;
    private volatile DefaultAsyncRequest _asyncRequest;

    public NettyDecoder(Class<T> dataType) {
        _dataType = dataType;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (_dataProtocol == null) {
            if (in.readableBytes() < 1)
                return;

            byte version = in.readByte();
            DataProtocol dataProtocol = DataProtocols.ALL.get(version);
            if (dataProtocol == null)
                throw new DataProtocolException("Unknown protocol: " + version);

            _dataProtocol = dataProtocol;
            _asyncRequest = new DefaultAsyncRequest();
        }

        DataProtocol dataProtocol = _dataProtocol;
        LengthCodec lengthCodec = dataProtocol.getLengthCodec();
        TransferCodec transferCodec = dataProtocol.getTransferCodec();
        while (in.readableBytes() > 0) {
            if (_asyncRequest.needNextTrunk()) {
                if (in.readableBytes() < lengthCodec.getLengthByteCount())
                    break;

                byte[] lengthBytes = new byte[lengthCodec.getLengthByteCount()];
                in.readBytes(lengthBytes);
                _asyncRequest.setCurrentTrunk(lengthBytes);
                _asyncRequest.completeCurrentTrunk();
                int length = lengthCodec.decode(lengthBytes);
                if (length == TransferCodec.FIN_LENGTH) {
                    _asyncRequest.ready();
                    T data = transferCodec.decode(_asyncRequest.getData(), _dataType);
                    addToOut(out, data, dataProtocol);
                    reset();
                    break;
                }

                _asyncRequest.setCurrentTrunk(new byte[length]);
            } else {
                byte[] dataBytes = _asyncRequest.getCurrentTrunk();
                int needRead = _asyncRequest.getCurrentTrunkRemaining();
                if (needRead > in.readableBytes())
                    needRead = in.readableBytes();
                in.readBytes(dataBytes, _asyncRequest.getCurrentTrunkRead(), needRead);
                _asyncRequest.addCurrentTrunkRead(needRead);
            }
        }
    }

    protected void addToOut(List<Object> out, T data, DataProtocol dataProtocol) {
        out.add(data);
    }

    private void reset() {
        _dataProtocol = null;
        _asyncRequest = null;
    }
}
