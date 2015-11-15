package org.bluemix.challenge.io;

import com.vaadin.server.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * Created by marco on 15/11/15.
 */
public interface ImageResource extends Serializable {
    InputStream EMPTY = new InputStream() {
        @Override
        public int read() throws IOException {
            return -1;
        }
    };

    OutputStream getOutputStream();

    InputStream getInputStream();

    Resource asVaadinResource();

    void destroy();
}
