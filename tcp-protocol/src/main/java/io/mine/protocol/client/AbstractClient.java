package io.mine.protocol.client;

import java.net.InetSocketAddress;
import java.util.Objects;

import io.mine.protocol.api.AbstractService;
import io.mine.protocol.data.DataProtocol;

/**
 * @author koqizhao
 *
 * Oct 11, 2018
 */
public abstract class AbstractClient<Req, Res> extends AbstractService<Req, Res>
        implements Client<Req, Res> {

    private InetSocketAddress _serverAddress;
    private DataProtocol _dataProtocol;

    public AbstractClient(Class<Req> requestType, Class<Res> responseType, InetSocketAddress serverAddress,
            DataProtocol dataProtocol) {
        super(requestType, responseType);

        Objects.requireNonNull(dataProtocol, "dataProcotol is null");
        Objects.requireNonNull(serverAddress, "serverAddress is null");

        _serverAddress = serverAddress;
        _dataProtocol = dataProtocol;
    }

    @Override
    public InetSocketAddress getServerAddress() {
        return _serverAddress;
    }

    @Override
    public DataProtocol getDataProtocol() {
        return _dataProtocol;
    }

}
