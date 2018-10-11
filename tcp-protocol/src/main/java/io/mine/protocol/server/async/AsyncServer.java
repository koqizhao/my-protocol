package io.mine.protocol.server.async;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import io.mine.protocol.api.Service;
import io.mine.protocol.codec.LengthCodec;
import io.mine.protocol.codec.TransferCodec;
import io.mine.protocol.data.DataProtocol;
import io.mine.protocol.data.DataProtocolException;
import io.mine.protocol.data.DataProtocols;
import io.mine.protocol.server.AbstractServer;

/**
 * @author koqizhao
 *
 * Oct 9, 2018
 */
public class AsyncServer<Req, Res> extends AbstractServer<Req, Res> {

    private ServerSocketChannel _serverSocketChannel;
    private Selector _selector;

    public AsyncServer(InetSocketAddress socketAddress, Service<Req, Res> service) {
        super(socketAddress, service);
    }

    @Override
    protected void doStart() throws IOException {
        _serverSocketChannel = ServerSocketChannel.open();
        _serverSocketChannel.configureBlocking(false);
        _serverSocketChannel.setOption(StandardSocketOptions.SO_RCVBUF, 32 * 1024);
        _serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        _serverSocketChannel.bind(getSocketAddress());

        _selector = Selector.open();
        _serverSocketChannel.register(_selector, SelectionKey.OP_ACCEPT);

        serve();
    }

    protected void serve() throws IOException {
        while (isStarted()) {
            try {
                _selector.select(1 * 1000);
            } catch (IOException e) {

            }

            Iterator<SelectionKey> selectedKeys = _selector.selectedKeys().iterator();
            while (selectedKeys.hasNext()) {
                SelectionKey selectionKey = selectedKeys.next();
                selectedKeys.remove();
                if (selectionKey.isAcceptable()) {
                    SocketChannel socketChannel = ((ServerSocketChannel) selectionKey.channel()).accept();
                    socketChannel.configureBlocking(false);
                    socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, false);
                    socketChannel.setOption(StandardSocketOptions.SO_RCVBUF, 32 * 1024);
                    socketChannel.setOption(StandardSocketOptions.SO_SNDBUF, 32 * 1024);
                    socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, false);
                    socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
                    socketChannel.register(_selector, SelectionKey.OP_READ, new ChannelData());
                    System.out.println("Connection established from client: " + socketChannel.getRemoteAddress());
                } else if (selectionKey.isReadable()) {
                    handleRead(selectionKey);
                } else if (selectionKey.isWritable()) {
                    handleWrite(selectionKey);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void handleRead(SelectionKey selectionKey) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        ChannelData channelData = (ChannelData) selectionKey.attachment();
        ByteBuffer buffer = channelData.buffer;
        try {
            int count;
            count = socketChannel.read(buffer);
            if (count == -1)
                throw new EOFException("Connection closed by client: " + socketChannel.getRemoteAddress());

            if (count == 0)
                return;

            buffer.flip();

            DefaultAsyncRequestContext context = channelData.context;
            if (context == null) {
                int version = buffer.get();
                DataProtocol dataProtocol = DataProtocols.ALL.get(version);
                if (dataProtocol == null)
                    throw new DataProtocolException("Unknown protocol: " + version);

                context = channelData.context = new DefaultAsyncRequestContext(dataProtocol);
            }

            DefaultAsyncRequest request = context.getRequest();
            DataProtocol dataProtocol = context.getDataProtocol();
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

                        context.getResponse().addCompleteListener(bytes -> {
                            buffer.put(dataProtocol.getVersion());
                            buffer.put(bytes);
                            buffer.flip();
                            selectionKey.interestOps(SelectionKey.OP_WRITE);
                            _selector.wakeup();
                        });

                        final DefaultAsyncRequestContext c = context;
                        getWorkerPool().submit(() -> handleRequest(c));
                        return;
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
        } catch (Exception e) {
            selectionKey.attach(null);
            selectionKey.cancel();
            try {
                socketChannel.close();
                e.printStackTrace();
                System.out.println("Connection failed with client");
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void handleWrite(SelectionKey selectionKey) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        ChannelData channelData = (ChannelData) selectionKey.attachment();
        ByteBuffer buffer = channelData.buffer;
        try {
            socketChannel.write(buffer);
            if (!buffer.hasRemaining()) {
                buffer.clear();
                channelData.context = null;
                selectionKey.interestOps(SelectionKey.OP_READ);
            }
        } catch (Exception e) {
            selectionKey.attach(null);
            selectionKey.cancel();
            try {
                socketChannel.close();
                e.printStackTrace();
                System.out.println("Connection failed with client: " + socketChannel.getRemoteAddress());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    protected void handleRequest(DefaultAsyncRequestContext context) {
        TransferCodec transferCodec = context.getDataProtocol().getTransferCodec();
        Res response = null;
        try {
            byte[] requestData = context.getRequest().getData();
            if (requestData != null) {
                Req request = transferCodec.decode(context.getRequest().getData(), getService().getRequestType());
                response = getService().invoke(request);
            }
        } catch (Exception ex) {
            System.out.println("handle request failed");
            ex.printStackTrace();
        } finally {
            byte[] responseData = transferCodec.encode(response);
            context.getResponse().complete(responseData);
        }
    }

    @Override
    protected void doStop() throws IOException {
        if (_serverSocketChannel != null) {
            _serverSocketChannel.close();
            _serverSocketChannel = null;
        }
    }

    protected class ChannelData {
        public final ByteBuffer buffer = ByteBuffer.allocate(32 * 1024);
        public DefaultAsyncRequestContext context;
    }

}