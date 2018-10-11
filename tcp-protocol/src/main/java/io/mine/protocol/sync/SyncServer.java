package io.mine.protocol.sync;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import io.mine.protocol.api.Service;
import io.mine.protocol.data.DataProtocol;
import io.mine.protocol.data.DataProtocolException;
import io.mine.protocol.data.DataProtocols;
import io.mine.protocol.server.AbstractServer;

/**
 * @author koqizhao
 *
 * Oct 9, 2018
 */
public class SyncServer<Req, Res> extends AbstractServer<Req, Res> {

    private ServerSocket _serverSocket;

    public SyncServer(InetSocketAddress socketAddress, Service<Req, Res> service) {
        super(socketAddress, service);
    }

    @Override
    protected void doStart() throws IOException {
        _serverSocket = new ServerSocket();
        _serverSocket.setSoTimeout(300 * 1000);
        _serverSocket.setReuseAddress(true);
        _serverSocket.setReceiveBufferSize(32 * 1024);
        _serverSocket.bind(getSocketAddress());

        serve();
    }

    protected void serve() throws IOException {
        while (isStarted()) {
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
                socket.setKeepAlive(false);
                socket.setReceiveBufferSize(32 * 1024);
                socket.setSendBufferSize(32 * 1024);
                getWorkerPool().submit(() -> serve(socket));
            } catch (Exception e) {
                e.printStackTrace();
                socket.close();
            }
        }
    }

    protected void serve(Socket socket) {
        try {
            SyncRequestContext context = new DefaultSyncRequestContext(socket.getInputStream(),
                    socket.getOutputStream());
            while (isStarted()) {
                int version = context.getInputStream().read();
                if (version == -1) {
                    System.out.println("Got EOF. Connection closed.");
                    break;
                }

                DataProtocol dataProtocol = DataProtocols.ALL.get(version);
                if (dataProtocol == null)
                    throw new DataProtocolException(
                            "Unsupported protocol version: " + version + " from " + socket.getRemoteSocketAddress());

                Req request = dataProtocol.getTransferCodec().decode(context.getInputStream(),
                        getService().getRequestType());
                Res response = getService().invoke(request);

                context.getOutputStream().write(version);
                dataProtocol.getTransferCodec().encode(context.getOutputStream(), response);
                context.getOutputStream().flush();
            }
        } catch (SocketTimeoutException e) {
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

    @Override
    protected void doStop() throws IOException {
        if (_serverSocket != null) {
            _serverSocket.close();
            _serverSocket = null;
        }
    }

}
