package io.mine.protocol.api.sample;

import io.mine.protocol.api.AbstractService;

/**
 * @author koqizhao
 *
 * Oct 11, 2018
 */
public class SampleService extends AbstractService<SampleRequest, SampleResponse> {

    public SampleService() {
        super(SampleRequest.class, SampleResponse.class);
    }

    @Override
    public SampleResponse invoke(SampleRequest request) {
        if (request == null)
            return null;

        SampleResponse response = new SampleResponse();
        response.setTime(System.currentTimeMillis());
        response.setFeedback(String.format("Nice! %s, your time: %s, my time: %s", request.getName(), request.getTime(),
                response.getTime()));
        return response;
    }

}
