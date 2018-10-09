package io.mine.protocol.server;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.mydotey.objectpool.facade.ThreadPools;
import org.mydotey.objectpool.threadpool.ThreadPool;
import org.mydotey.objectpool.threadpool.autoscale.AutoScaleThreadPoolConfig;

import io.mine.protocol.api.ServerRequest;
import io.mine.protocol.api.ServerResponse;
import io.mine.protocol.data.DataProtocol;
import io.mine.protocol.data.DataProtocols;

/**
 * @author koqizhao
 *
 * Oct 9, 2018
 */
public class SyncServer implements Closeable {

    private int _port;
    private ServerSocket _serverSocket;

    private AtomicBoolean _started;
    private ThreadPool _workerPool;

    public SyncServer(int port) {
        if (port < 0)
            throw new IllegalArgumentException("port < 0: " + port);

        _port = port;
        _started = new AtomicBoolean();
    }

    public void start() throws IOException {
        if (!_started.compareAndSet(false, true))
            return;

        _serverSocket = new ServerSocket();
        _serverSocket.setSoTimeout(10 * 1000);
        _serverSocket.setReuseAddress(true);
        _serverSocket.setReceiveBufferSize(32 * 1024);
        _serverSocket.bind(new InetSocketAddress(_port));

        AutoScaleThreadPoolConfig threadPoolConfig = ThreadPools.newAutoScaleThreadPoolConfigBuilder()
                .setCheckInterval(10).setMaxIdleTime(10 * 1000).setMaxSize(100).setMinSize(10).setQueueCapacity(10)
                .setScaleFactor(10).build();
        _workerPool = ThreadPools.newThreadPool(threadPoolConfig);

        while (_started.get()) {
            Socket socket;
            try {
                socket = _serverSocket.accept();
            } catch (SocketTimeoutException | SocketException e) {
                continue;
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }

            try {
                socket.setTcpNoDelay(true);
                socket.setSoTimeout(10 * 1000);
                socket.setKeepAlive(false);
                socket.setReceiveBufferSize(32 * 1024);
                socket.setSendBufferSize(32 * 1024);
                _workerPool.submit(() -> serve(socket));
            } catch (Exception e) {
                e.printStackTrace();
                socket.close();
            }
        }
    }

    protected void serve(Socket socket) {
        try (InputStream is = socket.getInputStream(); OutputStream os = socket.getOutputStream();) {
            while (_started.get()) {
                int version = is.read();
                if (version == -1) {
                    System.out.println("Got EOF. Connection closed.");
                    break;
                }

                DataProtocol dataProtocol = DataProtocols.All.get(version);
                if (dataProtocol == null) {
                    System.out.println(
                            "Unsupported protocol version: " + version + " from " + socket.getRemoteSocketAddress());
                    break;
                }

                ServerRequest request = dataProtocol.read(is, ServerRequest.class);
                ServerResponse response = new ServerResponse();
                response.setTime(System.currentTimeMillis());
                response.setFeedback(String.format("Nice! %s, your time: %s, my time: %s", request.getName(),
                        request.getTime(), response.getTime()));
                os.write(version);
                dataProtocol.write(os, response);
                os.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        if (!_started.compareAndSet(true, false))
            return;

        try {
            if (_serverSocket != null) {
                _serverSocket.close();
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

        _serverSocket = null;
        _workerPool = null;
    }

    @Override
    public void close() throws IOException {
        stop();
    }

}
