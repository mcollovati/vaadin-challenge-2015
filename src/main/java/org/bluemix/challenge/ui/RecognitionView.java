package org.bluemix.challenge.ui;

import com.vaadin.cdi.CDIView;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;

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
import org.vaadin.cdiviewmenu.ViewMenuItem;
import org.vaadin.spinkit.Spinner;
import org.vaadin.spinkit.SpinnerType;
import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.label.RichText;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;

import java.util.ArrayList;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

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
    private final MButton uploadImageBtn = new MButton("Upload another image", e -> getUI().getNavigator().navigateTo(UploadView.VIEW_NAME))
            .withStyleName(ValoTheme.BUTTON_LINK);
    private final MButton insightsBtn = new MButton("Proceed to Insights", e -> getUI().getNavigator().navigateTo(InsightsView.VIEW_NAME))
            .withStyleName(ValoTheme.BUTTON_LINK);


    @PostConstruct
    void initView() {
        withMargin(true).withFullWidth();
        addStyleName("two-columns recognition-view");


        message.setWidth(100, Unit.PERCENTAGE);
        message.setValue("Visual recognition in progress");
        message.setStyleName("progress-message");
        message.addStyleName(ValoTheme.LABEL_COLORED);

        //uploadedImage.setWidth(50, Unit.PERCENTAGE);
        uploadedImage.setWidth(100, Unit.PERCENTAGE);


        spinner.setVisible(false);
        recognitionResults.setVisible(true);


        //info.setSizeFull();
        info.withMarkDown(getClass().getResourceAsStream("recognition.md"));

        uploadImageBtn.setVisible(false);
        insightsBtn.setVisible(false);

        //add(new MVerticalLayout(info, uploadedImage)
        add(new MVerticalLayout(info, new MHorizontalLayout(
                        uploadImageBtn, insightsBtn))
                        .withMargin(false)
                        .withFullHeight().withFullWidth()
                        .alignAll(Alignment.TOP_CENTER),
                new MVerticalLayout(message, spinner).withFullHeight().withFullWidth()
                        .withMargin(false)
                        .alignAll(Alignment.TOP_CENTER)
                        .expand(new MHorizontalLayout(uploadedImage, recognitionResults)
                                .withStyleName("results")
                                .withFullWidth())
        );

        tweetList.setCaption("IBM Insights for Twitter");
        tweetList.setVisible(false);
        tweetList.setHeightUndefined();

        recognitionResults.setWidth(100, Unit.PERCENTAGE);
        //recognitionResults.setHeightUndefined();
        recognitionResults.setHeight(100, Unit.PERCENTAGE);
        /*
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
        */
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
        ////recognitionResults.setVisible(false);
        tweetList.setVisible(false);
        uploadImageBtn.setVisible(false);
        insightsBtn.setVisible(false);
    }

    @UIUpdate
    void onRecognitionSucceded(@Observes RecognitionSuccededEvent event) {
        spinner.setVisible(false);
        message.setValue("Recognition completed successfully");
        message.setStyleName(ValoTheme.LABEL_SUCCESS);
        recognitionResults.withImageResponse(event.getRecognitionResults());
        tweetList.setVisible(true);
        uploadImageBtn.setVisible(true);
        insightsBtn.setVisible(true);

        getUI().scrollIntoView(recognitionResults);
    }

    @UIUpdate
    void onRecognitionFailed(@Observes RecognitionFailedEvent event) {
        spinner.setVisible(false);
        message.setValue("Cannot perform visual recognition: " + event.getReason().getMessage());
        message.setStyleName(ValoTheme.LABEL_FAILURE);
        uploadImageBtn.setVisible(true);
        insightsBtn.setVisible(false);
        recognitionResults.clearTable();
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
        services.recognize(eventFile.getUploadedImage().getInputStream());
    }

}
