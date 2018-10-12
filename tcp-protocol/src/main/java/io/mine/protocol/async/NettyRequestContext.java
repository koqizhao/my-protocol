package io.mine.protocol.async;

import io.mine.protocol.data.DataProtocol;

/**
 * @author koqizhao
 *
 * Oct 12, 2018
 */
class NettyRequestContext<Req, Res> {

    private DataProtocol dataProtocol;
    private Req request;
    private Res response;

    public NettyRequestContext(DataProtocol dataProtocol) {
        this.dataProtocol = dataProtocol;
    }

    public Req getRequest() {
        return request;
    }

    public void setRequest(Req request) {
        this.request = request;
    }

    public Res getResponse() {
        return response;
    }

    public void setResponse(Res response) {
        this.response = response;
    }

    public DataProtocol getDataProtocol() {
        return dataProtocol;
    }

}
