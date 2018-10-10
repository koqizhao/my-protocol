package io.mine.protocol.server;

import java.io.Closeable;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.mydotey.objectpool.facade.ThreadPools;
import org.mydotey.objectpool.threadpool.ThreadPool;
import org.mydotey.objectpool.threadpool.autoscale.AutoScaleThreadPoolConfig;

import io.mine.protocol.data.DataProtocol;
import io.mine.protocol.data.DataProtocolException;
import io.mine.protocol.data.DataProtocols;

/**
 * @author koqizhao
 *
 * Oct 9, 2018
 */
public class AsyncServer implements Closeable {

    private int _port;
    private ServerSocketChannel _serverSocketChannel;
    private Selector _selector;

    private AtomicBoolean _started;
    private ThreadPool _workerPool;

    private ConcurrentHashMap<SocketChannel, ChannelData> _channelDataMap;

    public AsyncServer(int port) {
        if (port < 0)
            throw new IllegalArgumentException("port < 0: " + port);

        _port = port;
        _started = new AtomicBoolean();
        _channelDataMap = new ConcurrentHashMap<>();
    }

    public void start() throws IOException {
        if (!_started.compareAndSet(false, true))
            return;

        _serverSocketChannel = ServerSocketChannel.open();
        _serverSocketChannel.configureBlocking(false);
        _serverSocketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, false);
        _serverSocketChannel.setOption(StandardSocketOptions.SO_RCVBUF, 32 * 1024);
        _serverSocketChannel.setOption(StandardSocketOptions.SO_SNDBUF, 32 * 1024);
        _serverSocketChannel.setOption(StandardSocketOptions.TCP_NODELAY, false);
        _serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        _serverSocketChannel.bind(new InetSocketAddress(_port));

        _selector = Selector.open();
        _serverSocketChannel.register(_selector, SelectionKey.OP_ACCEPT);

        AutoScaleThreadPoolConfig threadPoolConfig = ThreadPools.newAutoScaleThreadPoolConfigBuilder()
                .setCheckInterval(10).setMaxIdleTime(10 * 1000).setMaxSize(100).setMinSize(10).setQueueCapacity(10)
                .setScaleFactor(10).build();
        _workerPool = ThreadPools.newThreadPool(threadPoolConfig);

        serve();
    }

    protected void serve() throws IOException {
        while (_started.get()) {
            try {
                _selector.select(10 * 1000);
            } catch (Exception e) {
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
                    socketChannel.register(_selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
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
                byteBuffer.compact();
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

                    if (!byteBuffer.hasRemaining())
                        return;
                }

                if (channelData.request.data == null) {
                    if (byteBuffer.remaining() >= 4) {
                        byte[] lengthBytes = new byte[4];
                        byteBuffer.get(lengthBytes);
                        int length = 10; // todo
                        if (length == 0) {
                            channelData.request.clear();
                            return;
                        }

                        channelData.request.data = new byte[length];
                    }
                }

                if (byteBuffer.hasRemaining()) {
                    int needRead = channelData.request.data.length - channelData.request.bytesHandled;
                    if (needRead > byteBuffer.remaining())
                        needRead = byteBuffer.remaining();
                    byteBuffer.get(channelData.request.data, channelData.request.bytesHandled, needRead);
                    channelData.request.bytesHandled += needRead;
                }

                if (channelData.request.bytesHandled == channelData.request.data.length) {
                    selectionKey.interestOps(SelectionKey.OP_WRITE);
                    // todo: to request obj and handle bussiness
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

    protected void handleWrite(SelectionKey selectionKey) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        try {
            ChannelData channelData = _channelDataMap.get(socketChannel);
            ByteBuffer byteBuffer = channelData.response.buffer;
            if (byteBuffer == null)
                return;

            socketChannel.write(byteBuffer);
            if (!byteBuffer.hasRemaining()) {
                channelData.response.clear();
                selectionKey.interestOps(SelectionKey.OP_READ);
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

    public void stop() {
        if (!_started.compareAndSet(true, false))
            return;

        try {
            if (_serverSocketChannel != null) {
                _serverSocketChannel.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (_workerPool != null) {
                _workerPool.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        _serverSocketChannel = null;
        _workerPool = null;
        _channelDataMap.clear();
    }

    @Override
    public void close() throws IOException {
        stop();
    }

    protected class ChannelData {
        public ConnectionData request = new ConnectionData();
        public ConnectionData response = new ConnectionData();
    }

    protected class ConnectionData {
        public ByteBuffer buffer = ByteBuffer.allocate(32 * 1024);
        public DataProtocol protocol;
        public byte[] data;
        public int bytesHandled;

        public void clear() {
            protocol = null;
            data = null;
            bytesHandled = 0;
        }
    }

}
