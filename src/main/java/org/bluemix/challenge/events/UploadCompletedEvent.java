package org.bluemix.challenge.events;

import java.io.File;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Created by marco on 31/10/15.
 */
@Getter
@RequiredArgsConstructor
public class UploadCompletedEvent {

    private final File uploadedImage;

}
