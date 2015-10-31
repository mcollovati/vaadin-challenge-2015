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
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FileResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

import org.bluemix.challenge.ServicesFacade;
import org.bluemix.challenge.events.RecognitionFailedEvent;
import org.bluemix.challenge.events.RecognitionSuccededEvent;
import org.bluemix.challenge.events.UploadCompletedEvent;
import org.bluemix.challenge.events.UploadStartedEvent;
import org.vaadin.cdiviewmenu.ViewMenuItem;
import org.vaadin.spinkit.Spinner;
import org.vaadin.spinkit.SpinnerType;
import org.vaadin.viritin.label.RichText;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;

import java.io.File;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Marco Collovati
 */
@Slf4j
@CDIView(RecognitionView.VIEW_NAME)
@ViewMenuItem(title = "Image recognition", icon = FontAwesome.EYE)
public class RecognitionView extends MHorizontalLayout implements View {

    public static final String VIEW_NAME = "recognition";

    @Inject
    private ServicesFacade services;

    private final RichText info = new RichText();
    private final VisualRecognitionTable recognitionResults = new VisualRecognitionTable();
    private final Image uploadedImage = new Image("");
    private final Label message = new Label();
    private final Spinner spinner = new Spinner(SpinnerType.CUBE_GRID);


    @PostConstruct
    void initView() {
        message.setWidth(100, Unit.PERCENTAGE);
        message.setValue("Visual recognition in progress");
        message.setStyleName(ValoTheme.LABEL_COLORED);

        spinner.setVisible(false);

        info.setSizeFull();
        info.withMarkDown(getClass().getResourceAsStream("recognition.md"));
        withFullHeight().withFullWidth().expand(
                new MVerticalLayout(info)
                        .withFullHeight().withFullWidth()
                        .alignAll(Alignment.TOP_CENTER)
                        .expand(uploadedImage),
                new MVerticalLayout(message, spinner).withFullHeight().withFullWidth()
                        .expand(recognitionResults)
        );
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {

    }

    public void onUploadStarted(@Observes UploadStartedEvent event) {
        uploadedImage.setVisible(false);
        uploadedImage.setSource(null);
        spinner.setVisible(false);
    }

    public void onRecognitionSucceded(@Observes RecognitionSuccededEvent event) {
        UI.getCurrent().access(() -> {
            spinner.setVisible(false);
            message.setValue("Recognition completed successfully");
            message.setStyleName(ValoTheme.LABEL_SUCCESS);
            recognitionResults.withImageResponse(event.getRecognitionResults());
        });
    }

    public void onRecognitionFailed(@Observes RecognitionFailedEvent event) {
        UI.getCurrent().access(() -> {
            spinner.setVisible(false);
            message.setValue("Cannot perform visual recognition: " + event.getReason().getMessage());
            message.setStyleName(ValoTheme.LABEL_FAILURE);
        });
    }

    public void onImageUploaded(@Observes UploadCompletedEvent event) {
        uploadedImage.setSource(new FileResource(event.getUploadedImage()));
        uploadedImage.setVisible(true);
        message.setValue("Upload completed. Starting visual recognition");
        message.setStyleName(ValoTheme.LABEL_SUCCESS);
        spinner.setVisible(true);
        doRecognition(event.getUploadedImage());
    }

    private void doRecognition(File uploadedFile) {
        log.debug("Starting recognition");
        services.recognize(uploadedFile.toPath());
    }

}