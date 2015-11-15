/* ====================================================================
 * Created on 31/10/15
 * Copyright (C) 2015 Insiel Mercato S.p.a.
 * <p>
 * org.bluemix.challenge.ui.ResultsView
 * <p>
 * Comments are welcome.
 * <p>
 * - Marco Collovati <marco.collovati@insielmercato.it>
 * ====================================================================
 */
package org.bluemix.challenge.ui;

import com.vaadin.cdi.CDIView;
import com.vaadin.event.ShortcutAction;
import com.vaadin.event.ShortcutListener;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.FileResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.server.ResourceReference;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

import org.bluemix.challenge.MyUI;
import org.bluemix.challenge.ServicesFacade;
import org.bluemix.challenge.cdi.UIUpdate;
import org.bluemix.challenge.events.RecognitionFailedEvent;
import org.bluemix.challenge.events.RecognitionSuccededEvent;
import org.bluemix.challenge.events.TweetsQueryFailedEvent;
import org.bluemix.challenge.events.TweetsQuerySuccededEvent;
import org.bluemix.challenge.events.UploadCompletedEvent;
import org.bluemix.challenge.events.UploadStartedEvent;
import org.bluemix.challenge.ui.components.TweetList;
import org.bluemix.challenge.ui.components.VisualRecognitionTable;
import org.vaadin.addons.coverflow.CoverFlow;
import org.vaadin.addons.coverflow.client.CoverflowStyle;
import org.vaadin.cdiviewmenu.ViewMenuItem;
import org.vaadin.spinkit.Spinner;
import org.vaadin.spinkit.SpinnerType;
import org.vaadin.viritin.label.RichText;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import static java.util.stream.Collectors.toList;

/**
 * @author Marco Collovati
 */
@Slf4j
@CDIView(RecognitionView.VIEW_NAME)
@ViewMenuItem(title = "Image recognition", icon = FontAwesome.EYE, order = 2)
public class RecognitionView extends MHorizontalLayout implements View {

    public static final String VIEW_NAME = "recognition";

    @Inject
    private ServicesFacade services;

    private final RichText info = new RichText();
    private final VisualRecognitionTable recognitionResults = new VisualRecognitionTable();
    private final TweetList tweetList = new TweetList();
    private final Image uploadedImage = new Image();
    private final Label message = new Label();
    private final Spinner spinner = new Spinner(SpinnerType.THREE_BOUNCE);


    @PostConstruct
    void initView() {
        withMargin(true).withFullWidth();
        //setMargin(true);
        //setSizeFull();
        addStyleName("two-columns recognition-view");


        message.setWidth(100, Unit.PERCENTAGE);
        message.setValue("Visual recognition in progress");
        message.setStyleName("progress-message");
        message.addStyleName(ValoTheme.LABEL_COLORED);

        uploadedImage.setWidth(100, Unit.PERCENTAGE);


        spinner.setVisible(false);
        recognitionResults.setVisible(false);


        //info.setSizeFull();
        info.withMarkDown(getClass().getResourceAsStream("recognition.md"));


        add(new MVerticalLayout(info, uploadedImage)
                        .withMargin(false)
                        .withFullHeight().withFullWidth()
                        .alignAll(Alignment.TOP_CENTER)
                        //.expand(uploadedImage)
                ,
                new MVerticalLayout(message, spinner).withFullHeight().withFullWidth()
                        .withMargin(false)
                        .alignAll(Alignment.TOP_CENTER)
                        .expand(new MHorizontalLayout(recognitionResults, tweetList)
                                .withStyleName("results")
                                .withFullWidth())
        );

        tweetList.setCaption("IBM Insights for Twitter");
        tweetList.setVisible(false);
        tweetList.setHeightUndefined();

        recognitionResults.setWidth(100, Unit.PERCENTAGE);
        recognitionResults.setHeightUndefined();
        recognitionResults.addShortcutListener(new ShortcutListener("", ShortcutAction.KeyCode.ENTER, new int[0]) {
            @Override
            public void handleAction(Object sender, Object target) {
                if (target == recognitionResults && recognitionResults.getValue() != null) {
                    tweetList.searchStarted();
                    getUI().scrollIntoView(tweetList);
                    services.searchTweets(recognitionResults.getValue());
                }
            }
        });

    }


    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {

    }


    @UIUpdate
    void onTweetsReceived(@Observes TweetsQuerySuccededEvent event) {
        tweetList.setVisible(true);
        tweetList.setTweets(event.getTweets());
    }

    @UIUpdate
    void onTweetsFailure(@Observes TweetsQueryFailedEvent event) {
        tweetList.setTweets(new ArrayList<>());
        message.setValue("Cannot get tweets for selected label: " + event.getReason().getMessage());
        message.setStyleName(ValoTheme.LABEL_FAILURE);
        getUI().scrollIntoView(message);
    }


    @UIUpdate
    void onUploadStarted(@Observes UploadStartedEvent event) {
        uploadedImage.setVisible(false);
        uploadedImage.setSource(null);
        spinner.setVisible(false);
        recognitionResults.setVisible(false);
        tweetList.setVisible(false);
    }

    @UIUpdate
    void onRecognitionSucceded(@Observes RecognitionSuccededEvent event) {
        spinner.setVisible(false);
        message.setValue("Recognition completed successfully");
        message.setStyleName(ValoTheme.LABEL_SUCCESS);
        recognitionResults.withImageResponse(event.getRecognitionResults());
        recognitionResults.setVisible(true);
        tweetList.setVisible(true);
        recognitionResults.focus();
        getUI().scrollIntoView(recognitionResults);
    }

    @UIUpdate
    void onRecognitionFailed(@Observes RecognitionFailedEvent event) {
        spinner.setVisible(false);
        message.setValue("Cannot perform visual recognition: " + event.getReason().getMessage());
        message.setStyleName(ValoTheme.LABEL_FAILURE);
        recognitionResults.setVisible(false);
        getUI().scrollIntoView(message);
    }


    @UIUpdate
    void onImageUploaded(@Observes UploadCompletedEvent event) {
        //uploadedImage.setSource(new FileResource(event.getUploadedImage()));
        uploadedImage.setSource(event.getUploadedImage().asVaadinResource());
        uploadedImage.setVisible(true);
        message.setValue("Upload completed. Starting visual recognition");
        message.setStyleName(ValoTheme.LABEL_SUCCESS);
        spinner.setVisible(true);
        getUI().scrollIntoView(message);
    }

    void doRecognition(@Observes UploadCompletedEvent eventFile) {
        log.debug("Starting recognition");
        //services.recognize(eventFile.getUploadedImage().toPath());
        services.recognize(eventFile.getUploadedImage().getInputStream());
    }

}
