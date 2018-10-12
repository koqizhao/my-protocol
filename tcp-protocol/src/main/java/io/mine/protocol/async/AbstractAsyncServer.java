package io.mine.protocol.async;

import java.net.InetSocketAddress;

import io.mine.protocol.api.AsyncService;
import io.mine.protocol.api.Service;
import io.mine.protocol.codec.TransferCodec;
import io.mine.protocol.server.AbstractServer;

/**
 * @author koqizhao
 *
 * Oct 12, 2018
 */
public abstract class AbstractAsyncServer<Req, Res> extends AbstractServer<Req, Res> {

    private boolean _isAsyncService;
    private AsyncService<Req, Res> _asyncService;

    public AbstractAsyncServer(InetSocketAddress socketAddress, Service<Req, Res> service) {
        super(socketAddress, service);

        if (service instanceof AsyncService) {
            _isAsyncService = true;
            _asyncService = (AsyncService<Req, Res>) service;
        }
    }

    protected boolean isAsyncService() {
        return _isAsyncService;
    }

    protected AsyncService<Req, Res> getAsyncService() {
        return _asyncService;
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

}
