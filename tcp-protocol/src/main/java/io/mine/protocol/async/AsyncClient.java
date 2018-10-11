package io.mine.protocol.async;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import io.mine.protocol.client.AbstractClient;
import io.mine.protocol.codec.LengthCodec;
import io.mine.protocol.codec.TransferCodec;
import io.mine.protocol.data.DataProtocol;
import io.mine.protocol.data.DataProtocolException;
import io.mine.protocol.data.DataProtocols;

/**
 * @author koqizhao
 *
 * Oct 11, 2018
 */
public class AsyncClient<Req, Res> extends AbstractClient<Req, Res> {

    private SocketChannel _socketChannel;
    private Selector _selector;
    private ChannelContext _channelContext = new ChannelContext();

    public AsyncClient(Class<Req> requestType, Class<Res> responseType, InetSocketAddress serverAddress,
            DataProtocol dataProtocol) throws IOException {
        super(requestType, responseType, serverAddress, dataProtocol);

        _socketChannel = SocketChannel.open();
        _socketChannel.configureBlocking(false);
        _socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, false);
        _socketChannel.setOption(StandardSocketOptions.SO_RCVBUF, 32 * 1024);
        _socketChannel.setOption(StandardSocketOptions.SO_SNDBUF, 32 * 1024);
        _socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, false);
        _socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);

        _selector = Selector.open();
        _socketChannel.register(_selector, SelectionKey.OP_CONNECT, new ChannelContext());

        _socketChannel.connect(getServerAddress());

        _selector.select();
        Iterator<SelectionKey> selectedKeys = _selector.selectedKeys().iterator();
        SelectionKey selectionKey = selectedKeys.next();
        selectedKeys.remove();
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        if (socketChannel.isConnectionPending())
            socketChannel.finishConnect();
    }

    @Override
    public Res invoke(Req request) {
        SelectionKey selectionKey = null;
        try {
            _socketChannel.register(_selector, SelectionKey.OP_WRITE);
            int count = _selector.select(10 * 1000);
            if (count == 0)
                throw new SocketTimeoutException();

            Iterator<SelectionKey> selectedKeys = _selector.selectedKeys().iterator();
            selectionKey = selectedKeys.next();
            selectedKeys.remove();
            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
            _channelContext.getBuffer().clear();
            _channelContext.getBuffer().put(getDataProtocol().getVersion());
            byte[] requestData = getDataProtocol().getTransferCodec().encode(request);
            _channelContext.getBuffer().put(requestData);
            _channelContext.getBuffer().flip();
            while (_channelContext.getBuffer().hasRemaining())
                socketChannel.write(_channelContext.getBuffer());
            _channelContext.getBuffer().clear();

            selectionKey.interestOps(SelectionKey.OP_READ);
            long now = System.currentTimeMillis();
            for (long elipsed = System.currentTimeMillis() - now; 100 * 1000
                    - elipsed > 0; elipsed = System.currentTimeMillis() - now) {
                count = _selector.select(1 * 1000);
                if (count == 0)
                    continue;

                selectedKeys = _selector.selectedKeys().iterator();
                selectionKey = selectedKeys.next();
                selectedKeys.remove();
                socketChannel = (SocketChannel) selectionKey.channel();
                Res response = handleRead(socketChannel, _channelContext);
                if (response != null)
                    return response;
            }

            throw new SocketTimeoutException();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            _channelContext.getBuffer().clear();
            _channelContext.setRequestContext(null);
        }
    }

    protected Res handleRead(SocketChannel socketChannel, ChannelContext channelContext) throws IOException {
        ByteBuffer buffer = channelContext.getBuffer();
        DefaultAsyncRequestContext requestContext = channelContext.getRequestContext();
        int count;
        count = socketChannel.read(buffer);
        if (count == -1)
            throw new EOFException("Connection closed by client: " + socketChannel.getRemoteAddress());

        if (count == 0)
            return null;

        buffer.flip();

        if (requestContext == null) {
            int version = buffer.get();
            DataProtocol dataProtocol = DataProtocols.ALL.get(version);
            if (dataProtocol == null)
                throw new DataProtocolException("Unknown protocol: " + version);

            requestContext = new DefaultAsyncRequestContext(dataProtocol);
            channelContext.setRequestContext(requestContext);
        }

        DefaultAsyncRequest request = requestContext.getRequest();
        DataProtocol dataProtocol = requestContext.getDataProtocol();
        LengthCodec lengthCodec = dataProtocol.getLengthCodec();
        while (buffer.hasRemaining()) {
            if (request.needNextTrunk()) {
                if (buffer.remaining() < lengthCodec.getLengthByteCount())
                    break;

                byte[] lengthBytes = new byte[lengthCodec.getLengthByteCount()];
                buffer.get(lengthBytes);
                request.setCurrentTrunk(lengthBytes);
                request.completeCurrentTrunk();
                int length = lengthCodec.decode(lengthBytes);
                if (length == TransferCodec.FIN_LENGTH) {
                    request.ready();
                    buffer.clear();
                    Res response = dataProtocol.getTransferCodec().decode(request.getData(), getResponseType());
                    if (response == null)
                        throw new DataProtocolException("Server returned null.");

                    return response;
                }

                request.setCurrentTrunk(new byte[length]);
            } else {
                byte[] dataBytes = request.getCurrentTrunk();
                int needRead = request.getCurrentTrunkRemaining();
                if (needRead > buffer.remaining())
                    needRead = buffer.remaining();
                buffer.get(dataBytes, request.getCurrentTrunkRead(), needRead);
                request.addCurrentTrunkRead(needRead);
            }
        }

        buffer.compact();
        return null;
    }

    @Override
    public void close() throws IOException {
        _socketChannel.close();
        _selector.close();
    }

}
