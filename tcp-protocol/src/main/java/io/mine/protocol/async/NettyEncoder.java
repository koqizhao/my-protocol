package io.mine.protocol.async;

import io.mine.protocol.data.DataProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

/**
 * @author koqizhao
 *
 * Oct 12, 2018
 */
class NettyEncoder<T> extends MessageToByteEncoder<T> {

    private AttributeKey<DataProtocol> _dataProtocolAttributeKey;

    public NettyEncoder(Class<T> dataType, AttributeKey<DataProtocol> dataProtocolAttributeKey) {
        super(dataType);

        _dataProtocolAttributeKey = dataProtocolAttributeKey;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, T msg, ByteBuf out) throws Exception {
        Attribute<DataProtocol> dataProtocolAttribute = ctx.channel().attr(_dataProtocolAttributeKey);
        DataProtocol dataProtocol = getDataProtocol(dataProtocolAttribute);
        byte[] responseData = dataProtocol.getTransferCodec().encode(msg);
        out.writeByte(dataProtocol.getVersion()).writeBytes(responseData);
    }

    protected DataProtocol getDataProtocol(Attribute<DataProtocol> dataProtocolAttribute) {
        return dataProtocolAttribute.get();
    }

}
