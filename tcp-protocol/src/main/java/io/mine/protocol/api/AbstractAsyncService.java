package io.mine.protocol.api;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author koqizhao
 *
 * Oct 11, 2018
 */
public abstract class AbstractAsyncService<Req, Res> extends AbstractService<Req, Res>
        implements AsyncService<Req, Res> {

    public AbstractAsyncService(Class<Req> requestType, Class<Res> responseType) {
        super(requestType, responseType);
    }

    @Override
    public Res invoke(Req request) {
        CompletableFuture<Res> future = invokeAsync(request);
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

}
