package io.mine.protocol.async;

import java.nio.ByteBuffer;

/**
 * @author koqizhao
 *
 * Oct 11, 2018
 */
class ChannelContext {

    private final ByteBuffer buffer = ByteBuffer.allocate(32 * 1024);

    private DefaultAsyncRequestContext requestContext;

    public DefaultAsyncRequestContext getRequestContext() {
        return requestContext;
    }

    public void setRequestContext(DefaultAsyncRequestContext requestContext) {
        this.requestContext = requestContext;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

}
