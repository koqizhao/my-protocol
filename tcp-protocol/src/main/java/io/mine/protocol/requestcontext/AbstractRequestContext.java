package io.mine.protocol.requestcontext;

import java.util.Objects;

import io.mine.protocol.data.DataProtocol;

/**
 * @author koqizhao
 *
 * Oct 11, 2018
 */
public abstract class AbstractRequestContext implements RequestContext {

    private DataProtocol _dataProtocol;

    public AbstractRequestContext(DataProtocol dataProtocol) {
        Objects.requireNonNull(dataProtocol, "dataProtocol is null");
        _dataProtocol = dataProtocol;
    }

    @Override
    public DataProtocol getDataProtocol() {
        return _dataProtocol;
    }

}
