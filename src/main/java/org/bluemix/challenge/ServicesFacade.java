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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import com.vaadin.ui.UI;

import org.apache.commons.lang3.time.DateUtils;
import org.bluemix.challenge.events.RecognitionFailedEvent;
import org.bluemix.challenge.events.RecognitionSuccededEvent;
import org.bluemix.challenge.events.TweetsQueryFailedEvent;
import org.bluemix.challenge.events.TweetsQuerySuccededEvent;
import org.watson.twitterinsights.DecahoseTwitterInsightsService;
import org.watson.twitterinsights.response.ResponseData;
import org.watson.visualrecognition.VisualRecognitionService;
import org.watson.visualrecognition.response.Image;
import org.watson.visualrecognition.response.Label;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Stateless;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Marco Collovati
 */
@Singleton
@Slf4j
public class ServicesFacade {

    //@Resource(lookup = "wm/MyWm")
    //private ManagedExecutorService executor;

    @Resource(lookup = "java:comp/DefaultManagedExecutorService")
    private ManagedExecutorService executor;

    @PostConstruct
    void init() {
        log.debug("Service thread " + Thread.currentThread().getContextClassLoader());
        executor.submit(() -> log.debug("Executor 1 thread " + Thread.currentThread().getContextClassLoader()) );
        //executor2.submit(() -> log.debug("Executor 2 thread " + Thread.currentThread().getContextClassLoader()) );

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


    private void onRecognitionSuccess(Image image) {
        recognitionSuccededEventEvent.fire(new RecognitionSuccededEvent(image));
    }

    private void onRecognitionFailed(Throwable t) {
        recognitionFailedEventEvent.fire(new RecognitionFailedEvent(t));
    }

    @Asynchronous
    public void recognize(Path uploadedFile) {
        log.debug("recognize thread " + Thread.currentThread().getContextClassLoader());
        try {
            log.debug("Starting visual recognition");
            Image r = visualRecognitionService.recognize(Files.readAllBytes(uploadedFile));
            log.debug("Visual recognition completed");
            recognitionSuccededEventEvent.fire(new RecognitionSuccededEvent(r));
        } catch (Throwable t) {
            recognitionFailedEventEvent.fire(new RecognitionFailedEvent(t));
        }
    }

    @Asynchronous
    public void searchTweets(Label label) {
        String startDate = LocalDate.now().minusDays(10).format(DateTimeFormatter.ISO_DATE);
        try {
            log.debug("Twitter insights query started");
            ResponseData r = twitterInsightsService.search(String.format("posted:%s %s", startDate, label.getLabelName()), 10, 0);
            log.debug("Twitter insights query completed");
            tweetsQuerySuccededEventEvent.fire(new TweetsQuerySuccededEvent(r.getTweets()));
        } catch (Throwable t) {
            log.debug("Twitter insights query failed", t);
            tweetsQueryFailedEventEvent.fire(new TweetsQueryFailedEvent(t));
        }
    }


    /*
    public void recognize(Path uploadedFile) {
        CompletableFuture.supplyAsync(() -> {
            log.debug("Executor 1 Ui: " + UI.getCurrent());
            try {
                log.debug("Starting visual recognition");
                return visualRecognitionService.recognize(Files.readAllBytes(uploadedFile));
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

        CompletableFuture.runAsync(() -> {
            log.debug("Executor 2 Ui: " + UI.getCurrent());
        },executor2);

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
    */
}
