package org.bluemix.challenge;

import com.vaadin.ui.UI;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.bluemix.challenge.cdi.UIAwareManagedExecutorService;
import org.bluemix.challenge.events.*;
import org.bluemix.challenge.io.ImageResource;
import org.bluemix.challenge.io.ZipUtils;
import org.watson.twitterinsights.DecahoseTwitterInsightsService;
import org.watson.visualinsights.VisualInsightsService;
import org.watson.visualrecognition.VisualRecognitionService;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.joining;

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
    private Event<RecognitionSuccededEvent> recognitionSuccededEvent;
    @Inject
    private Event<RecognitionFailedEvent> recognitionFailedEvent;
    @Inject
    private Event<TweetsQuerySuccededEvent> tweetsQuerySuccededEvent;
    @Inject
    private Event<TweetsQueryFailedEvent> tweetsQueryFailedEvent;
    @Inject
    private Event<VisualInsightsSuccededEvent> visualInsightsSuccededEvent;
    @Inject
    private Event<VisualInsightsFailedEvent> visualInsightsFailedEvent;

    @Inject
    VisualRecognitionService visualRecognitionService;

    @Inject
    DecahoseTwitterInsightsService twitterInsightsService;

    @Inject
    VisualInsightsService visualInsightsService;

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
                recognitionFailedEvent.fire(new RecognitionFailedEvent(t));
            } else {
                log.debug("Visual recognition completed");
                recognitionSuccededEvent.fire(new RecognitionSuccededEvent(r));
            }
        });
    }

    public void analyze(List<ImageResource> imageResources) {
        CompletableFuture.supplyAsync(() -> {
            // create zip
            AtomicInteger counter = new AtomicInteger();
            ZipUtils.ZipBuilder zipBuilder = ZipUtils.zip("images");
            imageResources.forEach(r -> zipBuilder.addEntry(String.format("image_%d.jpg", counter.getAndIncrement()), r.getInputStream()));
            ZipUtils.Zip zip = zipBuilder.build();
            try {
                return visualInsightsService.summary(zip.getInputStream());
            } finally {
                zip.destroy();
            }
        }, executor).whenComplete((d, t) -> {
            if (t != null) {
                log.debug("Visual insights failed", t);
                visualInsightsFailedEvent.fire(new VisualInsightsFailedEvent(t));
            } else {
                log.debug("Visual insights completed");
                visualInsightsSuccededEvent.fire(new VisualInsightsSuccededEvent(d));
            }
        });
    }

    private final static int RECORD_PER_PAGE = 20;

    public void searchTweets(Set<String> keywords) {
        String startDate = LocalDate.now().minusDays(10).format(DateTimeFormatter.ISO_DATE);
        String query = String.format("posted:%s (%s)", startDate, keywords.stream().collect(joining("\" OR \"", "\"", "\"")));
        //String query = String.format("posted:%s %s", startDate, String.join("' OR ", keywords));
        CompletableFuture.supplyAsync(() -> {
                    log.debug("Twitter insights query started");
                    int count = (int)twitterInsightsService.count(query);
                    return twitterInsightsService.search(query, RECORD_PER_PAGE, count - 100);
                }
                , executor).whenComplete((d, t) -> {
            if (t != null) {
                log.debug("Twitter insights query failed", t);
                tweetsQueryFailedEvent.fire(new TweetsQueryFailedEvent(t));
            } else {
                log.debug("Twitter insights query completed");
                tweetsQuerySuccededEvent.fire(new TweetsQuerySuccededEvent(d.getTweets()));
            }
        });
    }

}
