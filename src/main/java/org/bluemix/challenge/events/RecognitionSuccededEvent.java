/* ====================================================================
 * Created on 31/10/15
 * Copyright (C) 2015 Insiel Mercato S.p.a.
 * <p>
 * org.bluemix.challenge.events.RecognitionSuccededEvent
 * <p>
 * Comments are welcome.
 * <p>
 * - Marco Collovati <marco.collovati@insielmercato.it>
 * ====================================================================
 */
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
