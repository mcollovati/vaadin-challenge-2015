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
public class ServicesFacade {

    @Resource
    private ManagedExecutorService executor;
    //private final ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));

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

    public void recognize(Path uploadedFile) {
        log.debug("Starting visual recognition");

        CompletableFuture.supplyAsync(() -> {
            try {
                return visualRecognitionService.recognize(Files.readAllBytes(uploadedFile));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        },executor).whenComplete( (r,t) -> {
            if (t != null) {
                recognitionFailedEventEvent.fire(new RecognitionFailedEvent(t));
            } else {
                recognitionSuccededEventEvent.fire(new RecognitionSuccededEvent(r));
            }
        });

        /*
        ListenableFuture<Image> f = executor.submit(() -> visualRecognitionService.recognize(Files.readAllBytes(uploadedFile)));
        Futures.addCallback(f, new FutureCallback<Image>() {
            @Override
            public void onSuccess(final Image result) {
                log.debug("Visual recognition succeeded");
                onRecognitionSuccess(result);
            }

            @Override
            public void onFailure(final Throwable t) {
                log.error("Visual recognition failed", t);
                onRecognitionFailed(t);
            }
        });*/
    }


    public void searchTweets(Label label) {
        String startDate = LocalDate.now().minusDays(10).format(DateTimeFormatter.ISO_DATE);
        CompletableFuture.supplyAsync(() ->
            twitterInsightsService.search(String.format("posted:%s %s", startDate, label.getLabelName()),10, 0)
        ,executor).whenComplete( (d,t) -> {
            if (t != null) {
                tweetsQueryFailedEventEvent.fire(new TweetsQueryFailedEvent(t));
            } else {
                tweetsQuerySuccededEventEvent.fire(new TweetsQuerySuccededEvent(d.getTweets()));
            }
        });

    }
}
