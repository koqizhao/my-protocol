package io.mine.protocol.server.sync;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author koqizhao
 *
 * Oct 11, 2018
 */
public interface SyncRequestContext {

    InputStream getInputStream();

    OutputStream getOutputStream();

}
