package io.mine.protocol.sync;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author koqizhao
 *
 * Oct 11, 2018
 */
public class DefaultSyncRequestContext implements SyncRequestContext {

    private InputStream _inputStream;
    private OutputStream _outputStream;

    public DefaultSyncRequestContext(InputStream inputStream, OutputStream outputStream) {
        _inputStream = inputStream;
        _outputStream = outputStream;
    }

    @Override
    public InputStream getInputStream() {
        return _inputStream;
    }

    @Override
    public OutputStream getOutputStream() {
        return _outputStream;
    }

}
