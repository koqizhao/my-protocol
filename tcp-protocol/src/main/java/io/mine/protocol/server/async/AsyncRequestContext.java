package io.mine.protocol.server.async;

import io.mine.protocol.server.RequestContext;

/**
 * @author koqizhao
 *
 * Oct 11, 2018
 */
public interface AsyncRequestContext extends RequestContext {

    AsyncRequest getRequest();

    AsyncResponse getResponse();

}
