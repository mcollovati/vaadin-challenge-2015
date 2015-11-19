package org.bluemix.challenge.io;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Marco Collovati
 */
public class ZipUtils {

    @SneakyThrows
    public static ZipBuilder zip(String name) {
        Path temp = Files.createTempFile(Objects.requireNonNull(name), ".zip").toAbsolutePath();
        return new ZipBuilder(temp);
    }

    public static class ZipBuilder implements AutoCloseable {

        private final ZipOutputStream zos;
        private final Path zipPath;

        @SneakyThrows
        private ZipBuilder(Path tempFile) {
            this.zipPath = tempFile;
            this.zos = new ZipOutputStream(new FileOutputStream(this.zipPath.toFile()));
        }

        @SneakyThrows
        public ZipBuilder addEntry(String name, InputStream stream) {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(stream, "stream must not be null");
            zos.putNextEntry(new ZipEntry(name));
            IOUtils.copy(stream,zos);
            zos.closeEntry();
            return this;
        }

        @SneakyThrows
        public InputStream build() {
            close();
            return Files.newInputStream(zipPath);
        }

        @Override
        public void close() {
            IOUtils.closeQuietly(zos);
        }
    }
}
