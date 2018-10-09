package io.mine.protocol.client;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.ws.ProtocolException;

import io.mine.protocol.api.ServerRequest;
import io.mine.protocol.api.ServerResponse;
import io.mine.protocol.data.DataProtocol;
import io.mine.protocol.data.DataProtocols;

/**
 * @author koqizhao
 *
 * Oct 9, 2018
 */
public class SyncClient implements Closeable {

    private InetSocketAddress _server;
    private Socket _socket;
    private AtomicBoolean _connected;

    public SyncClient(InetSocketAddress server) {
        Objects.requireNonNull(server, "server is null");

        _server = server;
        _connected = new AtomicBoolean();
    }

    public void connect() throws UnknownHostException, IOException {
        if (!_connected.compareAndSet(false, true))
            return;

        _socket = new Socket();
        _socket.setTcpNoDelay(true);
        _socket.setKeepAlive(false);
        _socket.setSoTimeout(10 * 1000);
        _socket.setSendBufferSize(32 * 1024);
        _socket.setReceiveBufferSize(32 * 1024);
        _socket.connect(_server, 10 * 1000);
    }

    public ServerResponse send(ServerRequest request) throws IOException {
        OutputStream os = _socket.getOutputStream();
        os.write(DataProtocols.V0.getVersion());
        DataProtocols.V0.write(os, request);
        os.flush();

        InputStream is = _socket.getInputStream();
        int version = is.read();
        if (version == -1)
            throw new EOFException();

        DataProtocol protocol = DataProtocols.All.get(version);
        if (protocol == null)
            throw new ProtocolException(
                    "Unsupported protocol version: " + version + " from " + _socket.getRemoteSocketAddress());
        return protocol.read(is, ServerResponse.class);
    }

    @Override
    public void close() throws IOException {
        if (!_connected.compareAndSet(true, false))
            return;

        if (_socket != null) {
            _socket.close();
            _socket = null;
        }
    }

}
