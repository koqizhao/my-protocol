package io.mine.protocol.async;

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
import java.util.concurrent.CompletableFuture;

import io.mine.protocol.api.AsyncService;
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
    private boolean _isAsyncService;
    private AsyncService<Req, Res> _asyncService;

    public AsyncServer(InetSocketAddress socketAddress, Service<Req, Res> service) {
        super(socketAddress, service);

        if (service instanceof AsyncService) {
            _isAsyncService = true;
            _asyncService = (AsyncService<Req, Res>) service;
        }
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
                int count = _selector.select(1 * 1000);
                if (count == 0)
                    continue;
            } catch (IOException e) {
                continue;
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
                    socketChannel.register(_selector, SelectionKey.OP_READ, new ChannelContext());
                    System.out.println("Connection established from client: " + socketChannel.getRemoteAddress());
                } else if (selectionKey.isReadable()) {
                    handleRead(selectionKey);
                } else if (selectionKey.isWritable()) {
                    handleWrite(selectionKey);
                }
            }
        }
    }

    protected void handleRead(SelectionKey selectionKey) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        ChannelContext channelContext = (ChannelContext) selectionKey.attachment();
        ByteBuffer buffer = channelContext.getBuffer();
        try {
            int count;
            count = socketChannel.read(buffer);
            if (count == -1)
                throw new EOFException("Connection closed by client: " + socketChannel.getRemoteAddress());

            if (count == 0)
                return;

            buffer.flip();

            DefaultAsyncRequestContext requestContext = channelContext.getRequestContext();
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

                        requestContext.getResponse().addCompleteListener(bytes -> {
                            buffer.put(dataProtocol.getVersion());
                            buffer.put(bytes);
                            buffer.flip();
                            selectionKey.interestOps(SelectionKey.OP_WRITE);
                            _selector.wakeup();
                        });

                        final DefaultAsyncRequestContext c = requestContext;
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

    protected void handleWrite(SelectionKey selectionKey) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        ChannelContext channelContext = (ChannelContext) selectionKey.attachment();
        ByteBuffer buffer = channelContext.getBuffer();
        try {
            socketChannel.write(buffer);
            if (!buffer.hasRemaining()) {
                buffer.clear();
                channelContext.setRequestContext(null);
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

    protected void handleRequest(DefaultAsyncRequestContext requestContext) {
        TransferCodec transferCodec = requestContext.getDataProtocol().getTransferCodec();
        Req request = decode(transferCodec, requestContext.getRequest().getData());
        if (!_isAsyncService) {
            Res response = null;
            try {
                response = getService().invoke(request);
            } catch (Exception ex) {
                System.out.println("service execution failed");
                ex.printStackTrace();
            }

            byte[] responseData = encode(transferCodec, response);
            requestContext.getResponse().complete(responseData);
            return;
        }

        CompletableFuture<Res> future = _asyncService.invokeAsync(request);
        future.whenComplete((res, e) -> {
            if (e != null) {
                System.out.println("async service execution failed");
                e.printStackTrace();
                res = null;
            }

            byte[] responseData = encode(transferCodec, res);
            requestContext.getResponse().complete(responseData);
        });
    }

    protected Req decode(TransferCodec transferCodec, byte[] requestData) {
        Req request = null;
        try {
            request = transferCodec.decode(requestData, getService().getRequestType());
        } catch (Exception ex) {
            System.out.println("request decode failed");
            ex.printStackTrace();
        }

        return request;
    }

    protected byte[] encode(TransferCodec transferCodec, Res response) {
        byte[] responseData = null;
        try {
            responseData = transferCodec.encode(response);
        } catch (Exception ex) {
            System.out.println("request encode failed");
            ex.printStackTrace();
        }

        return responseData;
    }

    @Override
    protected void doStop() throws IOException {
        if (_serverSocketChannel != null) {
            _serverSocketChannel.close();
            _serverSocketChannel = null;
        }

        if (_selector != null) {
            _selector.close();
            _selector = null;
        }
    }

}