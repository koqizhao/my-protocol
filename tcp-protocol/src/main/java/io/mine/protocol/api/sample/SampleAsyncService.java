package io.mine.protocol.api.sample;

import java.util.concurrent.CompletableFuture;

import io.mine.protocol.api.AbstractAsyncService;

/**
 * @author koqizhao
 *
 * Oct 11, 2018
 */
public class SampleAsyncService extends AbstractAsyncService<SampleRequest, SampleResponse> {

    public SampleAsyncService() {
        super(SampleRequest.class, SampleResponse.class);
    }

    @Override
    public CompletableFuture<SampleResponse> invokeAsync(SampleRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            if (request == null)
                return null;

            SampleResponse response = new SampleResponse();
            response.setTime(System.currentTimeMillis());
            response.setFeedback(String.format("Nice! %s, your time: %s, my time: %s", request.getName(),
                    request.getTime(), response.getTime()));
            return response;
        });
    }

}
