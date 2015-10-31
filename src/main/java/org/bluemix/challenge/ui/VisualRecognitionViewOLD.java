package org.bluemix.challenge.ui;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import com.vaadin.cdi.CDIView;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptAll;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.event.dd.acceptcriteria.ClientSideCriterion;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FileResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.StreamVariable;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.Upload;
import com.vaadin.ui.themes.ValoTheme;

import org.apache.commons.io.FileUtils;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeTypes;
import org.bluemix.challenge.ServicesFacade;
import org.vaadin.cdiviewmenu.ViewMenuItem;
import org.vaadin.spinkit.Spinner;
import org.vaadin.spinkit.SpinnerType;
import org.vaadin.viritin.label.RichText;
import org.vaadin.viritin.layouts.MCssLayout;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.watson.visualrecognition.VisualRecognitionService;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import static org.fest.reflect.core.Reflection.field;

/**
 * Created by marco on 20/10/15.
 */
@Slf4j
//@CDIView("")
//@ViewMenuItem(icon = FontAwesome.IMAGE)
public class VisualRecognitionViewOLD { // extends MHorizontalLayout implements View {

    /*
    @Inject
    ServicesFacade services;

    private final Image uploadedImage = new Image("");
    private final Label progressMessage = new Label("");
    private final RichText info = new RichText();

    private final VisualRecognitionTable recognitionResults = new VisualRecognitionTable();

    private static final long MAX_UPLOAD_SIZE = 5 * 1024 * 1024;
    private Spinner spinner;

    @PostConstruct
    void initView() {
        setResponsive(true);
        setSizeFull();
        setMargin(true);
        final UploadAndRecognize uploadAndRecognize = new UploadAndRecognize();

        final ProgressBar uploadProgress = new ProgressBar();
        uploadProgress.setVisible(false);
        uploadProgress.setWidth(100, Unit.PERCENTAGE);

        spinner = new Spinner(SpinnerType.THREE_BOUNCE);
        spinner.addStyleName("wait-job");
        spinner.setVisible(false);

        info.setSizeFull();
        info.withMarkDown(getClass().getResourceAsStream("start.md"));


        final CustomUpload upload = new CustomUpload("Upload an image (max 5mb)", uploadAndRecognize);
        upload.setWidth("100%");
        upload.setImmediate(true);
        upload.addChangeListener(e -> {
            log.debug("Upload on change: {}", e.getFilename());
            recognitionResults.clearTable();
            progressMessage.setValue("");
            progressMessage.removeStyleName(ValoTheme.LABEL_SUCCESS);
            progressMessage.removeStyleName(ValoTheme.LABEL_FAILURE);
            uploadProgress.setValue(0f);
            uploadProgress.setVisible(true);
        });
        upload.addSucceededListener(uploadAndRecognize);
        upload.addFailedListener(uploadAndRecognize);
        upload.addStartedListener(event -> {
            if (event.getContentLength() > MAX_UPLOAD_SIZE) {
                progressMessage.setValue("File exceedes max upload size. File size is " + FileUtils.byteCountToDisplaySize(event.getContentLength()));
                progressMessage.setStyleName(ValoTheme.LABEL_FAILURE);
                upload.interruptUpload();
            } else {
                uploadProgress.setValue(0f);
                uploadProgress.setVisible(true);
                uploadProgress.setIndeterminate(event.getContentLength() < 0);
                log.debug("Started upload, content length is {}", event.getContentLength());

                spinner.setType(SpinnerType.THREE_BOUNCE);
                spinner.setVisible(true);
                progressMessage.setValue("Upload started");
            }
        });
        upload.addProgressListener((readBytes, contentLength) -> {
            boolean isInterrupted = field("interrupted").ofType(boolean.class).in(upload).get();

            if (isInterrupted) {
                log.debug("Upload is interrupted, do not show progress");
            } else {
                log.debug("Uploaded {} of {}", readBytes, contentLength);
                if (contentLength < 0 && readBytes > MAX_UPLOAD_SIZE) {
                    progressMessage.setValue("File exceedes max upload size. Actual file size is " + FileUtils.byteCountToDisplaySize(readBytes));
                    progressMessage.setStyleName(ValoTheme.LABEL_FAILURE);
                    upload.interruptUpload();
                } else if (contentLength > 0) {
                    float progress = readBytes / (contentLength * 1f);
                    uploadProgress.setValue(progress);
                    progressMessage.setValue("Uploaded " + FileUtils.byteCountToDisplaySize(readBytes) +
                            " of " + FileUtils.byteCountToDisplaySize(contentLength) +
                            " (" + progress + "%)");
                }
            }
        });
        upload.addFinishedListener(event -> {
            log.debug("Upload finished");
            upload.setEnabled(true);
            uploadProgress.setVisible(false);
        });

        DragAndDropWrapper uploadDrop = createFileDrop(upload);

        uploadedImage.addStyleName("uploaded-image-preview");
        uploadedImage.setVisible(false);
        uploadedImage.setWidth(100, Unit.PERCENTAGE);

        addDetachListener(event -> uploadAndRecognize.clear());


        recognitionResults.setSizeFull();


        add(info, new MVerticalLayout(upload, uploadDrop,uploadProgress, progressMessage, spinner, uploadedImage)
                        .withFullHeight().withFullWidth()
                        .expand(new CssLayout())
                        .alignAll(Alignment.MIDDLE_CENTER)
                , new MVerticalLayout().expand(recognitionResults)
                        .withFullHeight().withFullWidth()
        );//.expand(recognitionResults);
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {

    }


    class UploadAndRecognize implements Upload.Receiver, Upload.SucceededListener, Upload.FailedListener {

        private File uploadedFile;
        private final Metadata metadata = new Metadata();


        @Override
        public OutputStream receiveUpload(String filename, String mimeType) {
            try {
                uploadedFile = Files.createTempFile(filename, "").toFile();
                metadata.add(Metadata.CONTENT_TYPE, mimeType);
                return new FileOutputStream(uploadedFile);
            } catch (IOException ex) {
                // TODO handle error
                log.error("Cannot create upload output stream", ex);
            }
            return null;
        }

        @Override
        public void uploadSucceeded(Upload.SucceededEvent event) {
            uploadedImage.setSource(new FileResource(uploadedFile));
            uploadedImage.setCaption(event.getFilename());
            uploadedImage.setVisible(true);

            // Is an image
            boolean isImage = true;
            try {
                try (InputStream is = new BufferedInputStream(Files.newInputStream(uploadedFile.toPath()))) {
                    MediaType mediaType = MimeTypes.getDefaultMimeTypes().detect(is, metadata);
                    isImage = "image".equals(mediaType.getType());
                    if (!isImage) {
                        progressMessage.setValue("Uploaded file seems not to be an image. " +
                                "Detected media type is " + mediaType.toString());
                        progressMessage.setStyleName(ValoTheme.LABEL_FAILURE);
                        uploadFailed(new Upload.FailedEvent(event.getUpload(), event.getFilename(),
                                event.getMIMEType(), event.getLength(),
                                new RuntimeException("Invalid media type " + mediaType)));
                    }
                }
            } catch (IOException ex) {
                log.warn("Cannote detect mime type, proceed anyway", ex);
            }

            if (isImage) {
                // OK, invoke service
                progressMessage.setValue("Upload completed. Starting visual recognition");
                spinner.setType(SpinnerType.CUBE_GRID);
                log.debug("Starting recognition");
                services.recognize(uploadedFile.toPath(), result -> {
                    getUI().access(() -> {
                        progressMessage.setValue("Recognition completed successfully");
                        recognitionResults.withImageResponse(result);
                        spinner.setVisible(false);
                        //info.withMarkDown(VisualRecognitionView.this.getClass().getResourceAsStream("done.md"));
                    });
                }, t -> {
                    getUI().access(() -> {
                        progressMessage.setValue("Cannot perform visual recognition: " + t.getMessage());
                        progressMessage.setStyleName(ValoTheme.LABEL_FAILURE);
                        spinner.setVisible(false);
                    });
                });
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
            spinner.setVisible(false);
            log.warn("Upload failed", event.getReason());
            clear();
        }
    }

    class CustomUpload extends Upload {
        public CustomUpload(String caption, Receiver uploadReceiver) {
            super(caption, uploadReceiver);
        }

        @Override
        public StreamVariable getStreamVariable() {
            return super.getStreamVariable();
        }
    }

    private DragAndDropWrapper createFileDrop(CustomUpload upload) {
        DragAndDropWrapper dnd = new DragAndDropWrapper(new Label("Drop file here"));
        dnd.setDropHandler(new DropHandler() {
            @Override
            public void drop(DragAndDropEvent event) {
                DragAndDropWrapper.WrapperTransferable tr = (DragAndDropWrapper.WrapperTransferable) event.getTransferable();
                Arrays.stream(tr.getFiles()).forEach(h -> {
                    h.setStreamVariable(upload.getStreamVariable());
                });
            }

            @Override
            public AcceptCriterion getAcceptCriterion() {
                return AcceptAll.get();
            }
        });
        return dnd;
    }
*/
}
