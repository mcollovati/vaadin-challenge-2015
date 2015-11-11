/* ====================================================================
 * Created on 31/10/15
 * Copyright (C) 2015 Insiel Mercato S.p.a.
 * <p>
 * org.bluemix.challenge.ui.UploadView
 * <p>
 * Comments are welcome.
 * <p>
 * - Marco Collovati <marco.collovati@insielmercato.it>
 * ====================================================================
 */
package org.bluemix.challenge.ui;

import com.vaadin.cdi.CDIView;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptAll;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FileResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.ResourceReference;
import com.vaadin.server.StreamVariable;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import com.vaadin.ui.themes.ValoTheme;

import org.apache.commons.io.FileUtils;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeTypes;
import org.bluemix.challenge.MyUI;
import org.bluemix.challenge.events.UploadCompletedEvent;
import org.bluemix.challenge.events.UploadStartedEvent;
import org.vaadin.addons.coverflow.CoverFlow;
import org.vaadin.addons.coverflow.client.CoverflowStyle;
import org.vaadin.cdiviewmenu.ViewMenuItem;
import org.vaadin.spinkit.Spinner;
import org.vaadin.spinkit.SpinnerType;
import org.vaadin.viritin.label.RichText;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import static java.util.stream.Collectors.toList;
import static org.fest.reflect.core.Reflection.field;

/**
 * @author Marco Collovati
 */
@Slf4j
@CDIView(UploadView.VIEW_NAME)
@ViewMenuItem(title = "Upload image",icon = FontAwesome.UPLOAD,order = 1)
public class UploadView extends MHorizontalLayout implements View {

    public static final String VIEW_NAME = "upload";

    @Inject
    private javax.enterprise.event.Event<UploadStartedEvent> uploadStartedEventEvent;
    @Inject
    private javax.enterprise.event.Event<UploadCompletedEvent> uploadCompletedEventEvent;

    private static final long MAX_UPLOAD_SIZE = 5 * 1024 * 1024;
    private Spinner spinner;
    private final Label progressMessage = new Label("");
    private final RichText info = new RichText();
    private final ProgressBar uploadProgress = new ProgressBar();
    private final UploadAndRecognize uploadAndRecognize = new UploadAndRecognize();
    private CustomUpload upload;
    private final FilesystemCoverFlow coverFlow = new FilesystemCoverFlow();


    @PostConstruct
    void initView() {
        setSizeFull();
        //withFullWidth();
        setMargin(true);
        addStyleName("two-columns");

        coverFlow.setCoverflowStyle(CoverflowStyle.COVERFLOW);
        coverFlow.setMaxImageSize(300);
        coverFlow.setImmediate(true);

        /*coverFlow.addImageSelectionListener(ev ->
                coverFlow.getFileByIndex(ev.getSelectedIndex())
                        .ifPresent(this::startRecognition));*/

        uploadProgress.setVisible(false);
        uploadProgress.setWidth(100, Unit.PERCENTAGE);

        spinner = new Spinner(SpinnerType.THREE_BOUNCE);
        spinner.addStyleName("wait-job");
        spinner.setVisible(false);

        //info.setSizeFull();
        info.setWidth(100,Unit.PERCENTAGE);
        info.withMarkDown(getClass().getResourceAsStream("start.md"));



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


        add(info, new MVerticalLayout(upload, coverFlow, dropZone, uploadProgress, progressMessage, spinner)
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
        coverFlow.loadFromPath(((MyUI) UI.getCurrent()).getUploadFolder());
    }


    private static class FilesystemCoverFlow extends CoverFlow {

        private static final String RESOURCE_KEY = "coverFlowImage%d";
        public FilesystemCoverFlow() {
            super(Collections.emptyList());
        }

        Optional<File> getFileByIndex(int index) {
            return Optional.ofNullable(getResource(String.format(RESOURCE_KEY,index)))
                .map(r->(FileResource)r)
                .map(FileResource::getSourceFile);
        }

        void loadFromPath(Path path) {
            AtomicInteger counter = new AtomicInteger();
            List<String> urls = Stream.of(path.toFile().listFiles())
                    .filter(File::exists).filter(File::isFile)
                    .map(FileResource::new)
                    .peek( fr -> setResource(String.format(RESOURCE_KEY,counter.get()),fr) )
                    .map( fr -> ResourceReference.create(fr,this,String.format(RESOURCE_KEY,counter.getAndIncrement())).getURL() )
                    .map( url -> url.substring(5))
                    .collect(toList());
            setUrlList(urls);
            setVisible(!urls.isEmpty());
        }


    }

    private void startRecognition(File uploadedFile) {
        //Path pathToRemove = uploadedFile.toPath();
        //getUI().getSession().getService().addSessionDestroyListener(ev -> delete(pathToRemove) );
        getUI().getNavigator().navigateTo(RecognitionView.VIEW_NAME);
        uploadCompletedEventEvent.fire(new UploadCompletedEvent(uploadedFile));
    }

    class UploadAndRecognize implements Upload.Receiver, Upload.SucceededListener, Upload.FailedListener {

        private File uploadedFile;
        private final Metadata metadata = new Metadata();

        public UploadAndRecognize() {
        }

        @Override
        public OutputStream receiveUpload(String filename, String mimeType) {
            try {
                Path uploadFolder = ((MyUI)UI.getCurrent()).getUploadFolder();
                uploadedFile = Files.createTempFile(uploadFolder, filename, "").toFile();
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
                        delete(uploadedFile.toPath());
                        uploadFailed(new Upload.FailedEvent(event.getUpload(), event.getFilename(),
                                event.getMIMEType(), event.getLength(),
                                new RuntimeException("Invalid media type " + mediaType)));
                    }
                }
            } catch (IOException ex) {
                log.warn("Cannote detect mime type, proceed anyway", ex);
            }

            if (isImage) {
                startRecognition(uploadedFile);
                //Path pathToRemove = uploadedFile.toPath();
                //getUI().getSession().getService().addSessionDestroyListener(ev -> delete(pathToRemove) );
                //getUI().getNavigator().navigateTo(RecognitionView.VIEW_NAME);
                //uploadCompletedEventEvent.fire(new UploadCompletedEvent(uploadedFile));
            }
        }

        @Override
        public void uploadFailed(Upload.FailedEvent event) {
            spinner.setVisible(false);
            log.warn("Upload failed", event.getReason());
        }

    }

    private static void delete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            log.error("Cannot delete file "+path, ex);
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
}
