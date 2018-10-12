package io.mine.protocol.api;

import java.util.concurrent.CompletableFuture;

/**
 * @author koqizhao
 *
 * Oct 12, 2018
 */
public interface AsyncService<Req, Res> extends Service<Req, Res> {

    CompletableFuture<Res> invokeAsync(Req request);

}
