package org.bluemix.challenge.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Marco Collovati
 */
@RequiredArgsConstructor
public class VisualInsightsFailedEvent {
    @Getter
    private final Throwable reason;

}
