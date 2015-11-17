/* ====================================================================
 * Created on 16/11/15
 * Copyright (C) 2015 Insiel Mercato S.p.a.
 * <p>
 * org.bluemix.challenge.ui.VisualInsightsView
 * <p>
 * Comments are welcome.
 * <p>
 * - Marco Collovati <marco.collovati@insielmercato.it>
 * ====================================================================
 */
package org.bluemix.challenge.ui;

import com.vaadin.cdi.CDIView;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.MouseEventDetails;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.bluemix.challenge.ServicesFacade;
import org.bluemix.challenge.cdi.UIUpdate;
import org.bluemix.challenge.events.VisualInsightsFailedEvent;
import org.bluemix.challenge.events.VisualInsightsSuccededEvent;
import org.bluemix.challenge.io.ImageResource;
import org.bluemix.challenge.io.ImageStorage;
import org.bluemix.challenge.ui.components.TweetList;
import org.vaadin.cdiviewmenu.ViewMenuItem;
import org.vaadin.spinkit.Spinner;
import org.vaadin.spinkit.SpinnerType;
import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.button.PrimaryButton;
import org.vaadin.viritin.label.RichText;
import org.vaadin.viritin.layouts.MCssLayout;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;


/**
 * @author Marco Collovati
 */
@Slf4j
@CDIView(InsightsView.VIEW_NAME)
@ViewMenuItem(title = "Insights", icon = FontAwesome.IMAGE, order = 4)
public class InsightsView extends MHorizontalLayout implements View {

    public static final String VIEW_NAME = "insights";


    @Inject
    private ServicesFacade service;
    @Inject
    private ImageStorage imageStorage;
    private final Map<ImageResource, CardStatus> selectedResources = new HashMap<>();

    private MCssLayout gallery = new MCssLayout().withStyleName("gallery")
            .withFullWidth();
    private final TweetList tweetList = new TweetList();
    private PrimaryButton analyzeBtn;
    private Spinner analyzeSpinner = new Spinner(SpinnerType.THREE_BOUNCE);
    private Label analyzeLabel = new Label();


    @PostConstruct
    void initComponents() {
        withMargin(true).withFullWidth().withStyleName("insights-view");
        add(infoPanel(), visualInsightsPanel(), twitterInsightsPanel());
    }

    private Component twitterInsightsPanel() {
        tweetList.setCaption("IBM Insights for Twitter");
        tweetList.setVisible(false);
        tweetList.setHeightUndefined();
        return new CssLayout();
    }

    private Component visualInsightsPanel() {
        return new CssLayout();
    }

    private Layout infoPanel() {
        RichText info = new RichText().withMarkDown(getClass().getResourceAsStream("insights.md"));
        gallery.addStyleName(ValoTheme.LAYOUT_HORIZONTAL_WRAPPING);

        final MButton toggleAllImages = new MButton(FontAwesome.CHECK_SQUARE_O)
                .withCaption("Select/Deselect all");
        toggleAllImages.withListener(e -> {
            boolean newValue = !(boolean)toggleAllImages.getData();
            selectedResources.values().forEach(s -> s.setValue(newValue));
            toggleAllImages.setData(newValue);
            toggleAllImages.setIcon( (newValue) ? FontAwesome.SQUARE_O: FontAwesome.CHECK_SQUARE_O );
        }).withStyleName(ValoTheme.BUTTON_QUIET);
        toggleAllImages.setData(Boolean.FALSE);
        analyzeBtn = new PrimaryButton("Analyze", event -> startVisualInsight());
        analyzeBtn.setEnabled(false);
        analyzeSpinner.setVisible(false);
        analyzeSpinner.setWidth("100%");
        analyzeLabel.setVisible(false);
        MHorizontalLayout buttons = new MHorizontalLayout(toggleAllImages, analyzeBtn)
                .withMargin(false);
        return new MVerticalLayout(info, buttons, analyzeLabel, analyzeSpinner, gallery).withMargin(false);
    }

    private void startVisualInsight() {
        List<ImageResource> toAnalyze = selectedResources.entrySet().stream()
                .filter( e -> e.getValue().getValue())
                .map( e -> e.getKey())
                .collect(toList());
        if (!toAnalyze.isEmpty()) {
            analyzeSpinner.setVisible(true);
            analyzeLabel.setVisible(false);
            gallery.setEnabled(false);
            service.analyze(toAnalyze);
        }
    }

    @UIUpdate
    void onVisualInsightsSucceded(@Observes VisualInsightsSuccededEvent event) {
        analyzeSpinner.setVisible(false);
        analyzeLabel.setStyleName(ValoTheme.LABEL_SUCCESS);
        analyzeLabel.setValue("Visual insights extraction completed");
        analyzeLabel.setVisible(true);
        gallery.setEnabled(true);
    }
    @UIUpdate
    void onVisualInsightsFailed(@Observes VisualInsightsFailedEvent event) {
        analyzeSpinner.setVisible(false);
        analyzeLabel.setStyleName(ValoTheme.LABEL_FAILURE);
        analyzeLabel.setValue("Visual insights extraction failed. Please try again");
        analyzeLabel.setVisible(true);
        gallery.setEnabled(true);
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        selectedResources.clear();
        buildGallery();
    }

    private void buildGallery() {
        gallery.removeAllComponents();
        imageStorage.getResources().stream()
                .map(this::galleryImage)
                .forEach(c -> {
                    gallery.addComponent(c);
                });
    }

    private Component galleryImage(ImageResource imageResource) {
        Image image = new Image(imageResource.getDescription(), imageResource.asVaadinResource());
        image.setAlternateText(imageResource.getDescription());
        image.setDescription(imageResource.getDescription());
        image.setWidth(100, Unit.PERCENTAGE);

        MHorizontalLayout actions = new MHorizontalLayout()
                .withMargin(false).withFullWidth().withStyleName("gallery-image-actions")
                .expand(new CssLayout())
                .add(new MButton(FontAwesome.TRASH_O, e -> destroyResource(imageResource)).withDescription("Delete")
                        .withStyleName(ValoTheme.BUTTON_BORDERLESS));

        MCssLayout card = new MCssLayout(image, actions).withStyleName("gallery-image");
        CardStatus status = selectedResources.computeIfAbsent(imageResource, k -> {
            CardStatus cs = new CardStatus();
            cs.addValueChangeListener(e -> {
                card.setStyleName("selected", cs.getValue());
                analyzeBtn.setEnabled(selectedResources.values().stream().map(CardStatus::getValue)
                        .distinct().anyMatch( b -> b));
            });
            return cs;
        });
        card.setStyleName("selected", status.getValue());
        card.addLayoutClickListener(ev -> {
            if (ev.getButton() == MouseEventDetails.MouseButton.LEFT) {
                status.toggle();
            }
        });
        return card;
    }

    private void destroyResource(ImageResource imageResource) {
        imageStorage.destroy(imageResource);
        selectedResources.remove(imageResource);
        buildGallery();
    }

    private static class CardStatus extends ObjectProperty<Boolean> {

        public CardStatus() {
            super(false, Boolean.class);
        }

        public boolean toggle() {
            setValue(!getValue());
            return getValue();
        }

    }
}
