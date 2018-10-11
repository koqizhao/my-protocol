package io.mine.protocol.api;

/**
 * @author koqizhao
 *
 * Oct 11, 2018
 */
public interface Service<Req, Res> {

    Class<Req> getRequestType();

    Class<Res> getResponseType();

    Res invoke(Req request);

}
