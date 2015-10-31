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

import org.bluemix.challenge.events.RecognitionFailedEvent;
import org.bluemix.challenge.events.RecognitionSuccededEvent;
import org.watson.visualrecognition.VisualRecognitionService;
import org.watson.visualrecognition.response.Image;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

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

    private final ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));

    @Inject
    private Event<RecognitionSuccededEvent> recognitionSuccededEventEvent;
    @Inject
    private Event<RecognitionFailedEvent> recognitionFailedEventEvent;

    @Inject
    VisualRecognitionService visualRecognitionService;


    public void recognize(Path uploadedFile) {
        ListenableFuture<Image> f = executor.submit(() -> visualRecognitionService.recognize(Files.readAllBytes(uploadedFile)));
        Futures.addCallback(f, new FutureCallback<Image>() {
            @Override
            public void onSuccess(final org.watson.visualrecognition.response.Image result) {
                log.debug("Visual recognition succeeded");
                recognitionSuccededEventEvent.fire(new RecognitionSuccededEvent(result));
            }

            @Override
            public void onFailure(final Throwable t) {
                log.error("Visual recognition failed", t);
                recognitionFailedEventEvent.fire(new RecognitionFailedEvent(t));
            }
        });
    }


}
