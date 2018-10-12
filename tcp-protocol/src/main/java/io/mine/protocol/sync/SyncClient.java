package io.mine.protocol.sync;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import io.mine.protocol.client.AbstractClient;
import io.mine.protocol.data.DataProtocol;
import io.mine.protocol.data.DataProtocolException;
import io.mine.protocol.data.DataProtocols;

/**
 * @author koqizhao
 *
 * Oct 9, 2018
 */
public class SyncClient<Req, Res> extends AbstractClient<Req, Res> {

    private Socket _socket;

    public SyncClient(Class<Req> requestType, Class<Res> responseType, InetSocketAddress serverAddress,
            DataProtocol dataProtocol) throws UnknownHostException, IOException {
        super(requestType, responseType, serverAddress, dataProtocol);

        connect();
    }

    protected void connect() throws UnknownHostException, IOException {
        _socket = new Socket();
        _socket.setTcpNoDelay(true);
        _socket.setKeepAlive(false);
        _socket.setSoTimeout(30 * 1000);
        _socket.setSendBufferSize(32 * 1024);
        _socket.setReceiveBufferSize(32 * 1024);
        _socket.connect(getServerAddress(), 10 * 1000);
    }

    @Override
    public Res invoke(Req request) {
        try {
            SyncRequestContext context = new DefaultSyncRequestContext(_socket.getInputStream(),
                    _socket.getOutputStream());
            context.getOutputStream().write(getDataProtocol().getVersion());
            getDataProtocol().getTransferCodec().encode(context.getOutputStream(), request);
            context.getOutputStream().flush();

            int version = context.getInputStream().read();
            if (version == -1)
                throw new EOFException();

            DataProtocol protocol = DataProtocols.ALL.get(version);
            if (protocol == null)
                throw new DataProtocolException(
                        "Unsupported protocol version: " + version + " from " + _socket.getRemoteSocketAddress());
            Res response = protocol.getTransferCodec().decode(context.getInputStream(), getResponseType());
            if (response == null)
                throw new DataProtocolException("Server returned null.");

            return response;
        } catch (IOException e) {
            throw new DataProtocolException(e);
        }
    }

    @Override
    public void close() throws IOException {
        _socket.close();
    }

}
