/* ====================================================================
 * Created on 31/10/15
 * Copyright (C) 2015 Insiel Mercato S.p.a.
 * <p>
 * org.bluemix.challenge.ServicesFacade
 * <p>
 * Comments are welcome.
 * <p>
 * - Marco Collovati <marco.collovati@insielmercato.it>
 * ====================================================================
 */
package org.bluemix.challenge;

import com.vaadin.ui.UI;

import org.apache.commons.io.IOUtils;
import org.bluemix.challenge.cdi.UIAwareManagedExecutorService;
import org.bluemix.challenge.events.RecognitionFailedEvent;
import org.bluemix.challenge.events.RecognitionSuccededEvent;
import org.bluemix.challenge.events.TweetsQueryFailedEvent;
import org.bluemix.challenge.events.TweetsQuerySuccededEvent;
import org.watson.twitterinsights.DecahoseTwitterInsightsService;
import org.watson.visualrecognition.VisualRecognitionService;
import org.watson.visualrecognition.response.Label;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Marco Collovati
 */
@ApplicationScoped
@Slf4j
public class ServicesFacade implements Serializable {

    @Resource(lookup = "java:comp/DefaultManagedExecutorService")
    private ManagedExecutorService executor;

    @PostConstruct
    void init() {
        executor = UIAwareManagedExecutorService.makeUIAware(executor);
    }

    @Inject
    private Event<RecognitionSuccededEvent> recognitionSuccededEventEvent;
    @Inject
    private Event<RecognitionFailedEvent> recognitionFailedEventEvent;
    @Inject
    private Event<TweetsQuerySuccededEvent> tweetsQuerySuccededEventEvent;
    @Inject
    private Event<TweetsQueryFailedEvent> tweetsQueryFailedEventEvent;

    @Inject
    VisualRecognitionService visualRecognitionService;

    @Inject
    DecahoseTwitterInsightsService twitterInsightsService;

    public void recognize(InputStream inputStream) {
        CompletableFuture.supplyAsync(() -> {
            log.debug("Executor 1 Ui: " + UI.getCurrent());
            try {
                log.debug("Starting visual recognition");
                return visualRecognitionService.recognize(IOUtils.toByteArray(inputStream));
                        //Files.readAllBytes(uploadedFile));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, executor).whenComplete((r, t) -> {
            if (t != null) {
                log.debug("Visual recognition failed", t);
                recognitionFailedEventEvent.fire(new RecognitionFailedEvent(t));
            } else {
                log.debug("Visual recognition completed");
                recognitionSuccededEventEvent.fire(new RecognitionSuccededEvent(r));
            }
        });
    }


    public void searchTweets(Label label) {
        String startDate = LocalDate.now().minusDays(10).format(DateTimeFormatter.ISO_DATE);
        CompletableFuture.supplyAsync(() -> {
                    log.debug("Twitter insights query started");
                    return twitterInsightsService.search(String.format("posted:%s %s", startDate, label.getLabelName()), 10, 0);
                }
                , executor).whenComplete((d, t) -> {
            if (t != null) {
                log.debug("Twitter insights query failed", t);
                tweetsQueryFailedEventEvent.fire(new TweetsQueryFailedEvent(t));
            } else {
                log.debug("Twitter insights query completed");
                tweetsQuerySuccededEventEvent.fire(new TweetsQuerySuccededEvent(d.getTweets()));
            }
        });
    }

}
