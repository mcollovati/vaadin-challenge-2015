package org.bluemix.challenge.ui;

import com.vaadin.cdi.CDIView;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptAll;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.StreamVariable;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeTypes;
import org.bluemix.challenge.cdi.UIAwareManagedExecutorService;
import org.bluemix.challenge.events.UploadCompletedEvent;
import org.bluemix.challenge.events.UploadStartedEvent;
import org.bluemix.challenge.io.ImageResource;
import org.bluemix.challenge.io.ImageStorage;
import org.vaadin.cdiviewmenu.ViewMenuItem;
import org.vaadin.spinkit.Spinner;
import org.vaadin.spinkit.SpinnerType;
import org.vaadin.viritin.label.RichText;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static org.fest.reflect.core.Reflection.field;

/**
 * @author Marco Collovati
 */
@Slf4j
@CDIView(UploadView.VIEW_NAME)
@ViewMenuItem(title = "Upload image",icon = FontAwesome.UPLOAD,order = 1)
public class UploadView extends MHorizontalLayout implements View {

    public static final String VIEW_NAME = "upload";
    public static final String ADD_ACTION = VIEW_NAME + "/add";

    @Inject
    private javax.enterprise.event.Event<UploadStartedEvent> uploadStartedEventEvent;
    @Inject
    private javax.enterprise.event.Event<UploadCompletedEvent> uploadCompletedEventEvent;

    @Inject
    private ImageStorage imageStorage;

    @Resource(lookup = "java:comp/DefaultManagedExecutorService")
    private ManagedExecutorService executor;


    private static final long MAX_UPLOAD_SIZE = 5 * 1024 * 1024;
    private Spinner spinner;
    private final Label progressMessage = new Label("");
    private final RichText info = new RichText();
    private final ProgressBar uploadProgress = new ProgressBar();
    private final UploadAndRecognize uploadAndRecognize = new UploadAndRecognize();
    private CustomUpload upload;


    @PostConstruct
    void initView() {
        executor = UIAwareManagedExecutorService.makeUIAware(executor);
        withFullWidth().withMargin(true);
        addStyleName("two-columns upload-view");

        uploadProgress.setVisible(false);
        uploadProgress.setWidth(100, Unit.PERCENTAGE);

        spinner = new Spinner(SpinnerType.THREE_BOUNCE);
        spinner.addStyleName("wait-job");
        spinner.setVisible(false);

        info.setWidth(100,Unit.PERCENTAGE);
        info.withMarkDown(getClass().getResourceAsStream("upload.md"));

        upload =  new CustomUpload("Upload an image (max 5mb)", uploadAndRecognize);
        upload.setButtonCaption("Click to upload");
        upload.setWidth("100%");
        upload.setImmediate(true);
        upload.addChangeListener(e -> {
            log.debug("Upload on change: {}", e.getFilename());
            resetIndicators();
        });
        upload.addSucceededListener(uploadAndRecognize);
        upload.addFailedListener(uploadAndRecognize);
        upload.addStartedListener(event -> {
            resetIndicators();
            uploadStartedEventEvent.fire(new UploadStartedEvent());
            if (event.getContentLength() > MAX_UPLOAD_SIZE) {
                progressMessage.setValue("File exceedes max upload size. File size is " + FileUtils.byteCountToDisplaySize(event.getContentLength()));
                progressMessage.setStyleName(ValoTheme.LABEL_FAILURE);
                upload.interruptUpload();
            } else {

                UploadView.this.addStyleName("upload-started");

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
                            " (" + String.format("%.0f", progress * 100) + "%)");
                }
            }
        });
        upload.addFinishedListener(event -> {
            log.debug("Upload finished");
            upload.setEnabled(true);
            uploadProgress.setVisible(false);
            UploadView.this.removeStyleName("upload-started");
        });


        DragAndDropWrapper dropZone = createFileDrop(upload);


        add(info, new MVerticalLayout(upload, dropZone, uploadProgress, progressMessage, spinner)
                        .withStyleName("upload-container")
                        .withFullHeight().withFullWidth()
                        .expand(new CssLayout())
                        .alignAll(Alignment.MIDDLE_CENTER)
        );

    }

    private void resetIndicators() {
        progressMessage.setValue("");
        progressMessage.removeStyleName(ValoTheme.LABEL_SUCCESS);
        progressMessage.removeStyleName(ValoTheme.LABEL_FAILURE);
        uploadProgress.setValue(0f);
        uploadProgress.setVisible(true);
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        resetIndicators();
        uploadProgress.setVisible(false);
        upload.focus();

        if ("add".equals(event.getParameters())) {
            getUI().scrollIntoView(upload);
        }
    }


    private void startRecognition(ImageResource resource) {
        getUI().getNavigator().navigateTo(RecognitionView.VIEW_NAME);
        uploadCompletedEventEvent.fire(new UploadCompletedEvent(resource));
    }

    class UploadAndRecognize implements Upload.Receiver, Upload.SucceededListener, Upload.FailedListener {
        private Optional<ImageResource> resource;
        private final Metadata metadata = new Metadata();

        public UploadAndRecognize() {
        }

        @Override
        public OutputStream receiveUpload(String filename, String mimeType) {
            resource = imageStorage.createResource(filename);
            metadata.add(Metadata.CONTENT_TYPE, mimeType);
            return resource.map(ImageResource::getOutputStream).orElse(null);
        }

        @Override
        public void uploadSucceeded(Upload.SucceededEvent event) {
            boolean isImage = true;
            try {
                try (InputStream is = new BufferedInputStream(resource.map(ImageResource::getInputStream).orElse(ImageResource.EMPTY))) {
                    MediaType mediaType = MimeTypes.getDefaultMimeTypes().detect(is, metadata);
                    isImage = "image".equals(mediaType.getType());
                    if (!isImage) {
                        progressMessage.setValue("Uploaded file seems not to be an image. " +
                                "Detected media type is " + mediaType.toString());
                        progressMessage.setStyleName(ValoTheme.LABEL_FAILURE);
                        resource.ifPresent(imageStorage::destroy);
                        uploadFailed(new Upload.FailedEvent(event.getUpload(), event.getFilename(),
                                event.getMIMEType(), event.getLength(),
                                new RuntimeException("Invalid media type " + mediaType)));
                    }
                }
            } catch (IOException ex) {
                log.warn("Cannote detect mime type, proceed anyway", ex);
            }

            if (isImage) {
                resource.ifPresent(UploadView.this::startRecognition);
            }
        }

        @Override
        public void uploadFailed(Upload.FailedEvent event) {
            spinner.setVisible(false);
            resource.ifPresent(imageStorage::destroy);
            getUI().scrollIntoView(progressMessage);
            log.warn("Upload failed", event.getReason());
        }

    }

    // Expose getStreamVariable()
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
        Label dropFileLabel = new Label("or drop a file here");
        dropFileLabel.setStyleName(ValoTheme.LABEL_LARGE);
        Panel dropFilePanel = new Panel(dropFileLabel);
        dropFilePanel.setStyleName("upload-drop-zone");
        dropFilePanel.addStyleName(ValoTheme.PANEL_WELL);



        DragAndDropWrapper dnd = new DragAndDropWrapper(dropFilePanel);
        dnd.setDropHandler(new DropHandler() {
            @Override
            public void drop(DragAndDropEvent event) {
                DragAndDropWrapper.WrapperTransferable tr = (DragAndDropWrapper.WrapperTransferable) event.getTransferable();

                if (tr.getFiles() == null) {
                    progressMessage.setValue("Dropped content is not supported. Please drop an image file");
                    progressMessage.setStyleName(ValoTheme.LABEL_FAILURE);
                    spinner.setVisible(false);
                } else if (tr.getFiles().length > 1) {
                    progressMessage.setValue("Only one file at time could be uploaded");
                    progressMessage.setStyleName(ValoTheme.LABEL_FAILURE);
                    spinner.setVisible(false);
                } else {
                    Html5File file = tr.getFiles()[0];
                    if (file.getFileSize() > MAX_UPLOAD_SIZE) {
                        progressMessage.setValue("File exceedes max upload size. File size is " + FileUtils.byteCountToDisplaySize(file.getFileSize()));
                        progressMessage.setStyleName(ValoTheme.LABEL_FAILURE);
                    } else {
                        file.setStreamVariable(new StreamVariableWrapper(executor, upload.getStreamVariable()));
                    }
                }
            }

            @Override
            public AcceptCriterion getAcceptCriterion() {
                return AcceptAll.get();
            }
        });
        return dnd;
    }
    interface StreamVariableExclude {
        void streamingFinished(StreamVariable.StreamingEndEvent event);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private class StreamVariableWrapper implements StreamVariable {

        private final ExecutorService service;

        @Delegate(excludes = StreamVariableExclude.class)
        private final StreamVariable wrapped;

        @Override
        public void streamingFinished(StreamingEndEvent event) {
            service.execute(() -> this.deferStreamingFinished(event));
        }

        private void deferStreamingFinished(StreamingEndEvent event) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                // Ignore
            }
            UI.getCurrent().access( () -> wrapped.streamingFinished(event));
        }
    }
}
