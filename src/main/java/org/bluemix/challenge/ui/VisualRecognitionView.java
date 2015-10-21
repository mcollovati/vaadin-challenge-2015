package org.bluemix.challenge.ui;

import com.vaadin.cdi.CDIView;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FileResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Upload;

import org.vaadin.cdiviewmenu.ViewMenuItem;
import org.vaadin.viritin.fields.MTable;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.watson.visualrecognition.VisualRecognitionService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by marco on 20/10/15.
 */
@Slf4j
@CDIView("")
@ViewMenuItem(icon = FontAwesome.IMAGE)
public class VisualRecognitionView extends MHorizontalLayout implements View {

    @Inject
    VisualRecognitionService service;

    private final Image uploadedImage = new Image("Uploaded image");
    private final Label uploadStatus = new Label("");
    private final VisualRecognitionTable recognitionResults = new VisualRecognitionTable();

    @PostConstruct
    void init() {

        final UploadAndRecognize uploadAndRecognize = new UploadAndRecognize();

        Upload upload = new Upload("Upload an image", uploadAndRecognize);
        upload.setImmediate(true);
        upload.addSucceededListener(uploadAndRecognize);
        upload.addFailedListener(uploadAndRecognize);
        upload.addStartedListener(new Upload.StartedListener() {
            @Override
            public void uploadStarted(Upload.StartedEvent event) {
                uploadStatus.setValue("Upload started");
            }
        });

        uploadedImage.setVisible(false);
        uploadedImage.setWidth("100%");

        uploadStatus.setWidth("100%");

        addDetachListener(new DetachListener() {
            @Override
            public void detach(DetachEvent event) {
                uploadAndRecognize.clear();
            }
        });


        recognitionResults.setSizeFull();

        add(
                new MVerticalLayout(upload, uploadStatus, uploadedImage)
                        .withFullHeight().expand(uploadedImage),
                recognitionResults
        );//.expand(recognitionResults);
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {

    }

    class UploadAndRecognize implements Upload.Receiver, Upload.SucceededListener, Upload.FailedListener {

        private File uploadedFile;

        @Override
        public OutputStream receiveUpload(String filename, String mimeType) {
            try {
                uploadedFile = Files.createTempFile(filename, "").toFile();
                return new FileOutputStream(uploadedFile);
            } catch (IOException ex) {
                // TODO handle error
                log.error("Cannot create upload output stream", ex);
            }
            return null;
        }

        @Override
        public void uploadSucceeded(Upload.SucceededEvent event) {
            // OK, invoke service
            uploadedImage.setSource(new FileResource(uploadedFile));
            uploadedImage.setVisible(true);
            uploadStatus.setValue("Image upload completed");
            try {
                recognitionResults.withImageResponse(service.recognize(Files.readAllBytes(uploadedFile.toPath())));
            } catch (IOException e) {
                log.error("Cannot read file " + uploadedFile.getPath(), e);
            }
        }

        public void clear() {
            if (uploadedFile != null) {
                try {
                    Files.deleteIfExists(uploadedFile.toPath());
                } catch (IOException ex) {
                    log.error("Cannot delete temp file " + uploadedFile.getPath(), ex);
                }
            }
            uploadedFile = null;
        }

        @Override
        public void uploadFailed(Upload.FailedEvent event) {
            uploadedImage.setSource(null);
            uploadedImage.setVisible(false);
            log.warn("Upload failed", event.getReason());
            uploadStatus.setValue("Sorry, image upload failed");
            clear();
        }
    }
}
