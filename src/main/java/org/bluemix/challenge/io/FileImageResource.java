package org.bluemix.challenge.io;

import com.vaadin.server.FileResource;
import com.vaadin.server.Resource;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by marco on 15/11/15.
 */
@Slf4j
public class FileImageResource implements ImageResource {

    private final Path resource;

    FileImageResource(Path resource) {
        this.resource = Objects.requireNonNull(resource);
    }



    @Override
    public OutputStream getOutputStream() {
        try {
            return new FileOutputStream(resource.toFile());
        } catch (IOException ex) {
            log.error("Cannot create output stream for resource " + resource, ex);
        }
        return null;
    }

    @Override
    public InputStream getInputStream() {
        try {
            return new FileInputStream(resource.toFile());
        } catch (IOException ex) {
            log.error("Cannot create input stream for resource " + resource, ex);
        }
        return null;
    }

    @Override
    public Resource asVaadinResource() {
        return new FileResource(resource.toFile());
    }

    @Override
    public void destroy() {
        try {
            Files.deleteIfExists(resource);
        } catch (IOException ex) {
            log.error("Cannot destroy resource " + resource, ex);
        }
    }

}
