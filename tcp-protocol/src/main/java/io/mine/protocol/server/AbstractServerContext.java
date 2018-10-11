package io.mine.protocol.server;

import java.util.Objects;

import io.mine.protocol.data.DataProtocol;

/**
 * @author koqizhao
 *
 * Oct 11, 2018
 */
public abstract class AbstractServerContext implements RequestContext {

    private DataProtocol _dataProtocol;

    public AbstractServerContext(DataProtocol dataProtocol) {
        Objects.requireNonNull(dataProtocol, "dataProtocol is null");
        _dataProtocol = dataProtocol;
    }

    @Override
    public DataProtocol getDataProtocol() {
        return _dataProtocol;
    }

}
