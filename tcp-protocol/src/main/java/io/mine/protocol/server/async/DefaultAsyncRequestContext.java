package io.mine.protocol.server.async;

import io.mine.protocol.data.DataProtocol;
import io.mine.protocol.server.AbstractServerContext;

/**
 * @author koqizhao
 *
 * Oct 11, 2018
 */
public class DefaultAsyncRequestContext extends AbstractServerContext implements AsyncRequestContext {

    private DefaultAsyncRequest _asyncRequest;
    private DefaultAsyncResponse _asyncResponse;

    public DefaultAsyncRequestContext(DataProtocol dataProtocol) {
        super(dataProtocol);

        _asyncRequest = new DefaultAsyncRequest();
        _asyncResponse = new DefaultAsyncResponse();
    }

    @Override
    public DefaultAsyncRequest getRequest() {
        return _asyncRequest;
    }

    @Override
    public DefaultAsyncResponse getResponse() {
        return _asyncResponse;
    }

}
