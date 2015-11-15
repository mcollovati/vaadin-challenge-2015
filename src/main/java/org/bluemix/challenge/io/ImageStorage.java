package org.bluemix.challenge.io;

import java.util.List;
import java.util.Optional;

/**
 * Created by marco on 15/11/15.
 */
public interface ImageStorage {
    Optional<ImageResource> createResource(String name);

    List<ImageResource> getResources();

    void destroy();
}
