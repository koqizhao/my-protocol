package io.mine.protocol.server;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import io.mine.protocol.api.Service;
import io.mine.protocol.api.sample.SampleResponse;
import io.mine.protocol.codec.TransferCodec;
import io.mine.protocol.data.DataProtocol;
import io.mine.protocol.data.DataProtocolException;
import io.mine.protocol.data.DataProtocols;

/**
 * @author koqizhao
 *
 * Oct 9, 2018
 */
public class AsyncServer<Req, Res> extends AbstractServer<Req, Res> {

    private ServerSocketChannel _serverSocketChannel;
    private Selector _selector;

    private ConcurrentHashMap<SocketChannel, ChannelData> _channelDataMap;

    public AsyncServer(InetSocketAddress socketAddress, Service<Req, Res> service) {
        super(socketAddress, service);

        _channelDataMap = new ConcurrentHashMap<>();
    }

    @Override
    protected void doStart() throws IOException {
        _serverSocketChannel = ServerSocketChannel.open();
        _serverSocketChannel.configureBlocking(false);
        _serverSocketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, false);
        _serverSocketChannel.setOption(StandardSocketOptions.SO_RCVBUF, 32 * 1024);
        _serverSocketChannel.setOption(StandardSocketOptions.SO_SNDBUF, 32 * 1024);
        _serverSocketChannel.setOption(StandardSocketOptions.TCP_NODELAY, false);
        _serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        _serverSocketChannel.bind(getSocketAddress());

        _selector = Selector.open();
        _serverSocketChannel.register(_selector, SelectionKey.OP_ACCEPT);

        serve();
    }

    protected void serve() throws IOException {
        while (isStarted()) {
            try {
                _selector.select(10 * 1000);
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
                    _serverSocketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, false);
                    _serverSocketChannel.setOption(StandardSocketOptions.SO_RCVBUF, 32 * 1024);
                    _serverSocketChannel.setOption(StandardSocketOptions.SO_SNDBUF, 32 * 1024);
                    _serverSocketChannel.setOption(StandardSocketOptions.TCP_NODELAY, false);
                    _serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
                    _channelDataMap.put(socketChannel, new ChannelData());
                    socketChannel.register(_selector, SelectionKey.OP_READ);
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
        try {
            ChannelData channelData = _channelDataMap.get(socketChannel);
            ByteBuffer byteBuffer = channelData.request.buffer;
            int count;
            count = socketChannel.read(byteBuffer);
            if (count == -1)
                throw new EOFException("Connection closed by client: " + socketChannel.getRemoteAddress());

            if (count == 0)
                return;

            byteBuffer.flip();
            if (channelData.request.protocol == null) {
                int version = byteBuffer.get();
                channelData.request.protocol = DataProtocols.ALL.get(version);
                if (channelData.request.protocol == null)
                    throw new DataProtocolException("Unknown protocol: " + version);

                if (!byteBuffer.hasRemaining()) {
                    byteBuffer.compact();
                    return;
                }
            }

            while (byteBuffer.hasRemaining()) {
                if (channelData.request.needNext()) {
                    int lengthBytesCount = channelData.request.protocol.getLengthCodec().getLengthByteCount();
                    if (byteBuffer.remaining() < lengthBytesCount)
                        break;

                    byte[] lengthBytes = new byte[lengthBytesCount];
                    byteBuffer.get(lengthBytes);
                    channelData.request.data.add(lengthBytes);
                    channelData.request.totalHandled += lengthBytes.length;
                    int length = channelData.request.protocol.getLengthCodec().decode(lengthBytes);
                    if (length == TransferCodec.FIN_LENGTH) {
                        getWorkerPool().submit(() -> handleRequest(channelData));
                        selectionKey.interestOps(SelectionKey.OP_WRITE);
                        break;
                    }

                    channelData.request.data.add(new byte[length]);
                    channelData.request.currentHandled = 0;
                    channelData.request.totalHandled += length;
                } else {
                    byte[] dataBytes = channelData.request.data.get(channelData.request.data.size() - 1);
                    int needRead = dataBytes.length - channelData.request.currentHandled;
                    if (needRead > byteBuffer.remaining())
                        needRead = byteBuffer.remaining();
                    byteBuffer.get(dataBytes, channelData.request.currentHandled, needRead);
                    channelData.request.currentHandled += needRead;
                }
            }

            byteBuffer.compact();
        } catch (Exception e) {
            selectionKey.cancel();
            _channelDataMap.remove(socketChannel);
            try {
                socketChannel.close();
                e.printStackTrace();
                System.out.println("Connection failed with client: " + socketChannel.getRemoteAddress());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    protected void handleWrite(SelectionKey selectionKey) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        try {
            ChannelData channelData = _channelDataMap.get(socketChannel);
            ByteBuffer byteBuffer = channelData.response.buffer;
            if (channelData.response.needWrite == 0 || !byteBuffer.hasRemaining())
                return;

            synchronized (byteBuffer) {
                channelData.response.currentHandled += socketChannel.write(byteBuffer);
                if (channelData.response.currentHandled == channelData.response.needWrite) {
                    channelData.response.clear();
                    selectionKey.interestOps(SelectionKey.OP_READ);
                }
            }
        } catch (Exception e) {
            selectionKey.cancel();
            _channelDataMap.remove(socketChannel);
            try {
                socketChannel.close();
                e.printStackTrace();
                System.out.println("Connection failed with client: " + socketChannel.getRemoteAddress());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    protected void handleRequest(ChannelData data) {
        if (data.request.protocol == null) {
            SampleResponse serverResponse = new SampleResponse();
            serverResponse.setFeedback("Error: unknown request protocol!");
            serverResponse.setTime(System.currentTimeMillis());
            byte[] bytes = DataProtocols.V0.getTransferCodec().encode(serverResponse);

            synchronized (data.response.buffer) {
                data.response.buffer.clear();
                data.response.buffer.put(bytes);
                data.response.needWrite = bytes.length;
                data.response.buffer.flip();
            }

            return;
        }

        byte[] bytes = new byte[data.request.totalHandled];
        for (int i = 0, offset = 0; i < data.request.data.size(); i++) {
            byte[] dataBytes = data.request.data.get(i);
            System.arraycopy(dataBytes, 0, bytes, offset, dataBytes.length);
            offset += dataBytes.length;
        }

        Req request = data.request.protocol.getTransferCodec().decode(bytes, getService().getRequestType());
        Res response = getService().invoke(request);

        bytes = DataProtocols.V0.getTransferCodec().encode(response);
        synchronized (data.response.buffer) {
            data.response.buffer.clear();
            data.response.buffer.put(bytes);
            data.response.needWrite = bytes.length;
            data.response.buffer.flip();
        }
    }

    @Override
    protected void doStop() throws IOException {
        try {
            if (_serverSocketChannel != null) {
                _serverSocketChannel.close();
                _serverSocketChannel = null;
            }
        } finally {
            _channelDataMap.clear();
        }
    }

    protected class ChannelData {
        public ConnectionData request = new ConnectionData();
        public ConnectionData response = new ConnectionData();
    }

    protected class ConnectionData {
        public ByteBuffer buffer = ByteBuffer.allocate(32 * 1024);
        public DataProtocol protocol;
        public List<byte[]> data = new ArrayList<>();
        public int currentHandled;
        public int totalHandled;
        public int needWrite;

        public boolean needNext() {
            return data.size() == 0 || data.get(data.size() - 1).length == currentHandled;
        }

        public void clear() {
            protocol = null;
            data.clear();
            currentHandled = 0;
            totalHandled = 0;
            needWrite = 0;
        }
    }

}