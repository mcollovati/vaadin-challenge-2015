/* ====================================================================
 * Created on 17/11/15
 * Copyright (C) 2015 Insiel Mercato S.p.a.
 * <p>
 * org.bluemix.challenge.events.VisualInsightsFailedEvent
 * <p>
 * Comments are welcome.
 * <p>
 * - Marco Collovati <marco.collovati@insielmercato.it>
 * ====================================================================
 */
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
