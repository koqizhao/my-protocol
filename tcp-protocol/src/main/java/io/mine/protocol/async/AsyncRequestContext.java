package io.mine.protocol.async;

import io.mine.protocol.requestcontext.RequestContext;

/**
 * @author koqizhao
 *
 * Oct 11, 2018
 */
public interface AsyncRequestContext extends RequestContext {

    AsyncRequest getRequest();

    AsyncResponse getResponse();

}
