package io.mine.protocol.api;

import java.util.Objects;

/**
 * @author koqizhao
 *
 * Oct 11, 2018
 */
public abstract class AbstractService<Req, Res> implements Service<Req, Res> {

    private Class<Req> _requestType;
    private Class<Res> _responseType;

    public AbstractService(Class<Req> requestType, Class<Res> responseType) {
        Objects.requireNonNull(requestType, "requestType is null");
        Objects.requireNonNull(responseType, "responseType is null");

        _requestType = requestType;
        _responseType = responseType;
    }

    @Override
    public Class<Req> getRequestType() {
        return _requestType;
    }

    @Override
    public Class<Res> getResponseType() {
        return _responseType;
    }

}
