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
