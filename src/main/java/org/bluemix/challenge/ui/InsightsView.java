package org.bluemix.challenge.ui;

import com.vaadin.cdi.CDIView;
import com.vaadin.data.Property;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.event.ShortcutAction;
import com.vaadin.event.ShortcutListener;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.MouseEventDetails;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import lombok.extern.slf4j.Slf4j;
import org.bluemix.challenge.ServicesFacade;
import org.bluemix.challenge.cdi.UIUpdate;
import org.bluemix.challenge.events.TweetsQueryFailedEvent;
import org.bluemix.challenge.events.TweetsQuerySuccededEvent;
import org.bluemix.challenge.events.VisualInsightsFailedEvent;
import org.bluemix.challenge.events.VisualInsightsSuccededEvent;
import org.bluemix.challenge.io.ImageResource;
import org.bluemix.challenge.io.ImageStorage;
import org.bluemix.challenge.ui.components.TweetList;
import org.bluemix.challenge.ui.components.VisualInsightsChart;
import org.bluemix.challenge.ui.components.VisualInsightsTable;
import org.vaadin.cdiviewmenu.ViewMenuItem;
import org.vaadin.spinkit.Spinner;
import org.vaadin.spinkit.SpinnerType;
import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.label.RichText;
import org.vaadin.viritin.layouts.MCssLayout;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
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

    private Button analyzeBtn;
    private Spinner wipSpinner = new Spinner(SpinnerType.THREE_BOUNCE);
    private Label messageLabel = new Label();

    private VisualInsightsChart visualInsightsChart;
    private TabSheet insights;
    private TweetList tweetList;
    private VisualInsightsTable visualInsightsTable;


    @PostConstruct
    void initComponents() {
        withMargin(true).withFullWidth().withStyleName("insights-view");

        wipSpinner.setVisible(false);
        wipSpinner.setWidth("100%");
        messageLabel.setVisible(false);

        analyzeBtn = new MButton("Analyze", event -> {
            if (event.getButton() == analyzeBtn) {
                startVisualInsight();
            }
        }).withIcon(FontAwesome.SEARCH);
        analyzeBtn.setEnabled(false);

        add(infoPanel(), insightsPanel());
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        selectedResources.clear();
        buildGallery();
    }


    private Component twitterInsightsPanel() {
        tweetList.setVisible(false);
        return new CssLayout();
    }

    private Component insightsPanel() {
        visualInsightsChart = new VisualInsightsChart();
        visualInsightsChart.setSizeFull();
        visualInsightsChart.setVisible(false);

        tweetList = new TweetList();
        tweetList.setVisible(false);
        tweetList.setCaption("IBM Insights for Twitter");
        tweetList.setHeightUndefined();
        tweetList.setWidth(100, Unit.PERCENTAGE);
        tweetList.setCaption("");
        tweetList.addStyleName(ValoTheme.PANEL_BORDERLESS);

        visualInsightsTable = new VisualInsightsTable();
        visualInsightsTable.setWidth(100, Unit.PERCENTAGE);
        visualInsightsTable.setHeightUndefined();
        visualInsightsTable.setVisible(false);
        visualInsightsTable.addShortcutListener(new ShortcutListener("", ShortcutAction.KeyCode.ENTER, new int[0]) {
            @Override
            public void handleAction(Object sender, Object target) {
                if (target == visualInsightsTable && visualInsightsTable.getValue() != null) {
                    Set<String> tags = (Set<String>) visualInsightsTable.getConvertedValue();
                    tweetList.searchStarted(tags);
                    toggleAllTabs(false);
                    serviceStarted();
                    service.searchTweets(tags);
                }
            }
        });


        insights = new TabSheet();
        insights.setStyleName(ValoTheme.TABSHEET_CENTERED_TABS);
        insights.setSizeFull();


        TabSheet.Tab tab = insights.addTab(visualInsightsTable, "Visual Insights");
        tab.setEnabled(false);
        tab = insights.addTab(visualInsightsChart, "Visual Insights chart");
        tab.setEnabled(false);
        tab = insights.addTab(tweetList, "IBM Twitter Insights");
        tab.setEnabled(false);

        return new MVerticalLayout(messageLabel, wipSpinner).withMargin(false).withFullWidth().expand(insights);
    }

    private void toggleAllTabs(boolean enabled) {
        IntStream.range(0, insights.getComponentCount())
                .mapToObj(insights::getTab).forEach(t -> t.setEnabled(enabled));
    }

    private Layout infoPanel() {
        RichText info = new RichText().withMarkDown(getClass().getResourceAsStream("insights.md"));
        gallery.addStyleName(ValoTheme.LAYOUT_HORIZONTAL_WRAPPING);

        final MButton toggleAllImages = new MButton(FontAwesome.CHECK_SQUARE_O)
                .withCaption("Select/Deselect all");
        toggleAllImages.withListener(e -> {
            boolean newValue = !(boolean) toggleAllImages.getData();
            selectedResources.values().forEach(s -> s.setValue(newValue));
            toggleAllImages.setData(newValue);
            toggleAllImages.setIcon((newValue) ? FontAwesome.SQUARE_O : FontAwesome.CHECK_SQUARE_O);
        }).withStyleName(ValoTheme.BUTTON_QUIET);
        toggleAllImages.setData(Boolean.FALSE);
        MHorizontalLayout buttons = new MHorizontalLayout(toggleAllImages, analyzeBtn)
                .withMargin(false);
        //return new MVerticalLayout(info, buttons, messageLabel, wipSpinner, gallery).withMargin(false);
        return new MVerticalLayout(info, buttons, gallery).withMargin(false);
    }

    private void startVisualInsight() {
        List<ImageResource> toAnalyze = selectedResources.entrySet().stream()
                .filter(e -> e.getValue().getValue())
                .map(e -> e.getKey())
                .collect(toList());
        if (!toAnalyze.isEmpty()) {
            serviceStarted();
            service.analyze(toAnalyze);
            visualInsightsChart.empty();
            visualInsightsTable.empty();
            tweetList.empty();
            insights.setSelectedTab(visualInsightsTable);
            //insights.getTab(visualInsightsTable).setEnabled(false);
            //insights.getTab(visualInsightsChart).setEnabled(false);
            toggleAllTabs(false);
        }
    }


    @UIUpdate
    void onVisualInsightsSucceded(@Observes VisualInsightsSuccededEvent event) {
        serviceSuccess("Visual insights extraction completed");
        visualInsightsChart.withSummaryList(event.getSummaries());
        visualInsightsTable.withSummaryList(event.getSummaries());
        insights.getTab(visualInsightsTable).setEnabled(true);
        insights.getTab(visualInsightsChart).setEnabled(true);
        insights.setSelectedTab(visualInsightsTable);
        visualInsightsTable.focus();
    }

    @UIUpdate
    void onVisualInsightsFailed(@Observes VisualInsightsFailedEvent event) {
        serviceFailed("Visual insights extraction failed. Please try again");
        visualInsightsChart.empty();
        visualInsightsTable.empty();
        insights.getTab(visualInsightsTable).setEnabled(false);
        insights.getTab(visualInsightsChart).setEnabled(false);
        insights.setSelectedTab(visualInsightsTable);
    }

    @UIUpdate
    void onTweetsReceived(@Observes TweetsQuerySuccededEvent event) {
        serviceSuccess("Twitter insights query completed");
        tweetList.setTweets(event.getTweets());
        toggleAllTabs(true);
        insights.setSelectedTab(tweetList);
    }

    @UIUpdate
    void onTweetsFailure(@Observes TweetsQueryFailedEvent event) {
        serviceFailed("Cannot get tweets for selected label: " + event.getReason().getMessage());
        tweetList.setTweets(new ArrayList<>());
        insights.setSelectedTab(tweetList);
        insights.getTab(tweetList).setEnabled(false);
    }


    private void serviceStarted() {
        wipSpinner.setVisible(true);
        messageLabel.setVisible(false);
        gallery.setEnabled(false);
        analyzeBtn.setEnabled(false);
    }

    private void serviceFailed(String message) {
        wipSpinner.setVisible(false);
        messageLabel.setStyleName(ValoTheme.LABEL_FAILURE);
        messageLabel.setValue(message);
        messageLabel.setVisible(true);
        analyzeBtn.setEnabled(hasSelectedCards());
        gallery.setEnabled(true);
    }

    private void serviceSuccess(String message) {
        wipSpinner.setVisible(false);
        messageLabel.setStyleName(ValoTheme.LABEL_SUCCESS);
        messageLabel.setValue(message);
        messageLabel.setVisible(true);
        analyzeBtn.setEnabled(hasSelectedCards());
        gallery.setEnabled(true);
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


        String btnStyles = Stream.of(ValoTheme.BUTTON_BORDERLESS, ValoTheme.BUTTON_ICON_ONLY, ValoTheme.BUTTON_SMALL)
                .collect(joining(" "));
        MHorizontalLayout actions = new MHorizontalLayout()
                .withSpacing(false).withMargin(false).withFullWidth().withStyleName("gallery-image-actions")
                .expand(new CssLayout())
                .add(
                        new MButton(FontAwesome.TAGS, e -> Notification.show("Show tags")).withDescription("Visual recognition results")
                                .withStyleName(btnStyles),
                        new MButton(FontAwesome.TWITTER, e -> Notification.show("Start TwitterInsights")).withDescription("Search tweets")
                                .withStyleName(btnStyles),
                        new MButton(FontAwesome.TRASH_O, e -> destroyResource(imageResource)).withDescription("Delete image")
                                .withStyleName(btnStyles)
                );

        MCssLayout card = new MCssLayout(image, actions).withStyleName("gallery-image");
        CardStatus status = selectedResources.computeIfAbsent(imageResource, k -> {
            CardStatus cs = new CardStatus();
            cs.addValueChangeListener(e -> {
                analyzeBtn.setEnabled(hasSelectedCards());
            });
            return cs;
        });
        Property.ValueChangeListener toggleCardStatusListener = e -> card.setStyleName("selected", status.getValue());
        status.addValueChangeListener(toggleCardStatusListener);
        card.addDetachListener( e -> status.removeValueChangeListener(toggleCardStatusListener));

        card.setStyleName("selected", status.getValue());
        card.addLayoutClickListener(ev -> {
            if (ev.getButton() == MouseEventDetails.MouseButton.LEFT) {
                status.toggle();
            }
        });
        return card;
    }


    private boolean hasSelectedCards() {
        return selectedResources.values().stream().map(CardStatus::getValue)
                .distinct().anyMatch(b -> b);
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
