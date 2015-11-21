package org.bluemix.challenge.io;

import com.vaadin.cdi.UIScoped;

import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by marco on 15/11/15.
 */
@UIScoped
@Slf4j
public class FileImageStorage implements ImageStorage {

    private final Path storagePath;
    private final List<ImageResource> resources = new ArrayList<>();

    @SneakyThrows
    public FileImageStorage() {
        this.storagePath = Files.createTempDirectory(UUID.randomUUID().toString());
    }

    @Override
    public List<ImageResource> getResources() {
        return Collections.unmodifiableList(resources);
    }

    @Override
    public void destroy(ImageResource resource) {
        resources.remove(resource);
        if (resource instanceof FileImageResource) {
            ((FileImageResource)resource).destroy();;
        }
    }

    @Override
    public Optional<ImageResource> createResource(String name) {
        String filename = FilenameUtils.getBaseName(name);
        String ext = FilenameUtils.getExtension(name);
        if (!ext.isEmpty()) {
            ext = "." + ext;
        }
        try {
            FileImageResource resource = new FileImageResource(Files.createTempFile(storagePath, filename, ext), filename+ext);
            resources.add(resource);
            return Optional.of(resource);
        } catch (IOException ex) {
            log.error("Cannot create image resource", ex);
        }
        return Optional.empty();
    }


    @Override
    public void destroy() {
        try {
            Files.walkFileTree(storagePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException e)
                        throws IOException {
                    if (e == null) {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    } else {
                        // directory iteration failed
                        throw e;
                    }
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    // Ignore error
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            log.error("Cannot purge upload folder " + storagePath, ex);
        }

    }

    @Override
    public boolean isEmpty() {
        return getResources().isEmpty();
    }
}
