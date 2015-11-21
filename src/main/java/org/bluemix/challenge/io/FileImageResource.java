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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

/**
 * Created by marco on 15/11/15.
 */
@Slf4j
public class FileImageResource implements ImageResource {

    private final Path resource;
    private final String fileName;


    FileImageResource(Path resource, String fileName) {
        this.resource = Objects.requireNonNull(resource);
        this.fileName = Objects.requireNonNull(fileName);
    }


    @Override
    public String getDescription() {
        return fileName;
    }

    @Override
    public String size() {
        return FileUtils.byteCountToDisplaySize(resource.toAbsolutePath().toFile().length());
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

    void destroy() {
        try {
            Files.deleteIfExists(resource);
        } catch (IOException ex) {
            log.error("Cannot destroy resource " + resource, ex);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileImageResource resource1 = (FileImageResource) o;
        return Objects.equals(resource, resource1.resource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resource);
    }
}
