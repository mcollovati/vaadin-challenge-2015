package org.bluemix.challenge.events;

import org.watson.visualrecognition.response.Image;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Marco Collovati
 */
@RequiredArgsConstructor
@Getter
public class RecognitionSuccededEvent {
    private final Image recognitionResults;

}
