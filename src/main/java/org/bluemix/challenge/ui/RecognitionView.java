package org.bluemix.challenge.ui;

import com.vaadin.cdi.CDIView;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
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
@ViewMenuItem(title = "Image recognition", icon = FontAwesome.IMAGE, order = 2)
public class RecognitionView extends MHorizontalLayout implements View, Page.BrowserWindowResizeListener {

    public static final String VIEW_NAME = "recognition";

    @Inject
    private ServicesFacade services;

    private final RichText info = new RichText();
    private final VisualRecognitionTable recognitionResults = new VisualRecognitionTable();
    private final Image uploadedImage = new Image();
    private final Label message = new Label();
    private final Spinner spinner = new Spinner(SpinnerType.THREE_BOUNCE);
    private final MButton uploadImageBtn = new MButton("Upload another image", e -> getUI().getNavigator().navigateTo(UploadView.VIEW_NAME))
            .withVisible(false)
            .withIcon(FontAwesome.ARROW_LEFT);
    //.withStyleName(ValoTheme.BUTTON_LINK);
    private final MButton insightsBtn = new MButton("Proceed to Insights", e -> getUI().getNavigator().navigateTo(InsightsView.VIEW_NAME))
            .withVisible(false)
            .withIcon(FontAwesome.ARROW_RIGHT)
            .withStyleName(ValoTheme.BUTTON_ICON_ALIGN_RIGHT);


    private TabSheet tabSheet;

    @PostConstruct
    void initView() {
        withMargin(true).withFullWidth();
        addStyleName("two-columns recognition-view");


        message.setWidth(100, Unit.PERCENTAGE);
        message.setValue("Visual recognition in progress");
        message.setStyleName("progress-message");
        message.addStyleName(ValoTheme.LABEL_COLORED);

        tabSheet = new TabSheet();
        tabSheet.addSelectedTabChangeListener( e -> {
            // awful workaround
            Page.getCurrent().getJavaScript().execute("setTimeout(vaadin.forceLayout, 200);");
        });
        tabSheet.setStyleName(ValoTheme.TABSHEET_CENTERED_TABS);
        tabSheet.setWidth("100%");
        //tabSheet.setSizeFull();


        uploadedImage.setWidth(100, Unit.PERCENTAGE);

        spinner.setVisible(false);

        initRecognitionResultsTable();

        info.withMarkDown(getClass().getResourceAsStream("recognition.md"));

        add(new MVerticalLayout(info).withMargin(false),
                new MVerticalLayout(message, spinner,
                        new MHorizontalLayout(
                                uploadImageBtn, insightsBtn)
                                .withMargin(false)
                                .withStyleName("recognition-actions")
                                .withFullHeight().withFullWidth()
                                .alignAll(Alignment.TOP_CENTER)
                ).withFullHeight().withFullWidth()
                        .withMargin(false)
                        .alignAll(Alignment.TOP_CENTER)
                        .expand(tabSheet)
                        .withHeight("-1")
        );


        TabSheet.Tab tab = tabSheet.addTab(uploadedImage, "Uploaded Image", FontAwesome.IMAGE);
        tab.setEnabled(false);
        tab = tabSheet.addTab(recognitionResults, "Visual recognition", FontAwesome.LIST_ALT);
        tab.setEnabled(false);
    }

    private void initRecognitionResultsTable() {
        recognitionResults.setVisible(true);
        recognitionResults.setWidth(100, Unit.PERCENTAGE);
        recognitionResults.setHeightUndefined();
        ////recognitionResults.setSizeFull();
        recognitionResults.setSelectionMode(Grid.SelectionMode.NONE);
        //recognitionResults.setHeight(100, Unit.PERCENTAGE);
    }


    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        Page page = Page.getCurrent();
        browserWindowResized(new Page.BrowserWindowResizeEvent(page, page.getBrowserWindowWidth(), page.getBrowserWindowHeight()));
    }

    @Override
    public void attach() {
        super.attach();
        Page.getCurrent().addBrowserWindowResizeListener(this);
    }

    @Override
    public void detach() {
        Page.getCurrent().removeBrowserWindowResizeListener(this);
        super.detach();
    }

    @UIUpdate
    void onUploadStarted(@Observes UploadStartedEvent event) {
        uploadedImage.setVisible(false);
        uploadedImage.setSource(null);
        tabSheet.getTab(uploadedImage).setEnabled(false);
        tabSheet.getTab(recognitionResults).setEnabled(false);

        spinner.setVisible(false);

        /*
        tweetList.setVisible(false);
        uploadImageBtn.setVisible(false);
        */
        insightsBtn.setVisible(false);
    }

    @UIUpdate
    void onRecognitionSucceded(@Observes RecognitionSuccededEvent event) {
        spinner.setVisible(false);
        message.setValue("Recognition completed successfully");
        message.setStyleName(ValoTheme.LABEL_SUCCESS);
        recognitionResults.withImageResponse(event.getRecognitionResults());

        tabSheet.getTab(recognitionResults).setEnabled(true);
        tabSheet.setSelectedTab(recognitionResults);

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

        tabSheet.getTab(recognitionResults).setEnabled(false);
        recognitionResults.clearTable();

        getUI().scrollIntoView(message);
    }


    @UIUpdate
    void onImageUploaded(@Observes UploadCompletedEvent event) {
        uploadedImage.setSource(event.getUploadedImage().asVaadinResource());
        uploadedImage.setVisible(true);
        tabSheet.getTab(uploadedImage).setEnabled(true);
        tabSheet.setSelectedTab(uploadedImage);

        message.setValue("Upload completed. Starting visual recognition");
        message.setStyleName(ValoTheme.LABEL_SUCCESS);
        spinner.setVisible(true);
        getUI().scrollIntoView(uploadedImage);
    }

    void doRecognition(@Observes UploadCompletedEvent eventFile) {
        log.debug("Starting recognition");
        services.recognize(eventFile.getUploadedImage().getInputStream());
    }

    @Override
    public void browserWindowResized(Page.BrowserWindowResizeEvent event) {
        if (event.getWidth() <= 380) {
            uploadImageBtn.setCaption("Upload");
            insightsBtn.setCaption("Insights");
            recognitionResults.getColumn(VisualRecognitionTable.STARS_COLUMN).setHidden(true);
        } else {
            uploadImageBtn.setCaption("Upload another image");
            insightsBtn.setCaption("Proceed to Insights");
            recognitionResults.getColumn(VisualRecognitionTable.STARS_COLUMN).setHidden(false);
        }
    }
}
