package org.bluemix.challenge.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Marco Collovati
 */
@RequiredArgsConstructor
@Getter
public class RecognitionFailedEvent {
    private final Throwable reason;
}
