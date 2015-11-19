package org.bluemix.challenge.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.watson.visualinsights.response.Summary;

import java.util.List;

/**
 * @author Marco Collovati
 */
@RequiredArgsConstructor
public class VisualInsightsSuccededEvent {
    @Getter
    private final List<Summary> summaries;
}
