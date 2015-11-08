/* ====================================================================
 * Created on 07/11/15
 * Copyright (C) 2015 Insiel Mercato S.p.a.
 * <p>
 * org.bluemix.challenge.events.TweetsQuerySuccededEvent
 * <p>
 * Comments are welcome.
 * <p>
 * - Marco Collovati <marco.collovati@insielmercato.it>
 * ====================================================================
 */
package org.bluemix.challenge.events;

import org.watson.twitterinsights.response.Tweet;

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Marco Collovati
 */
@RequiredArgsConstructor
public class TweetsQuerySuccededEvent {
    @Getter
    private final List<Tweet> tweets;
}
