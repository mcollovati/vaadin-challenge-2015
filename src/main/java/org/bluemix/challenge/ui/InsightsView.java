package org.bluemix.challenge.ui;

import com.vaadin.cdi.CDIView;
import com.vaadin.data.Property;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.event.Action;
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
import org.bluemix.challenge.ui.components.ExternalLinkTarget;
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
import static java.util.stream.Collectors.toSet;


/**
 * @author Marco Collovati
 */
@Slf4j
@CDIView(InsightsView.VIEW_NAME)
@ViewMenuItem(title = "Insights", icon = FontAwesome.LIST_ALT, order = 4)
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
    private MButton toggleAllImages;


    @PostConstruct
    void initComponents() {
        withMargin(true).withFullWidth().withStyleName("two-columns insights-view");
        addStyleName(ValoTheme.LAYOUT_HORIZONTAL_WRAPPING);

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

    private Component insightsPanel() {
        visualInsightsChart = new VisualInsightsChart();
        visualInsightsChart.setSizeFull();
        visualInsightsChart.setVisible(false);

        tweetList = new TweetList();
        tweetList.setVisible(false);
        tweetList.setWidth(100, Unit.PERCENTAGE);
        tweetList.addStyleName(ValoTheme.PANEL_BORDERLESS);


        Action searchTweetsAction = new Action("Search tweets");

        visualInsightsTable = new VisualInsightsTable();
        visualInsightsTable.setWidth(100, Unit.PERCENTAGE);
        visualInsightsTable.setHeightUndefined();
        visualInsightsTable.setVisible(false);
        visualInsightsTable.setPageLength(0);
        visualInsightsTable.addActionHandler(new Action.Handler(){
            @Override
            public Action[] getActions(Object target, Object sender) {
                return new Action[] { searchTweetsAction };
            }

            @Override
            public void handleAction(Action action, Object sender, Object target) {
                if (action == searchTweetsAction) {
                    Set<String> tags = visualInsightsTable.getConvertedValue();
                    if (sender == visualInsightsTable && !tags.isEmpty()) {
                        tweetList.searchStarted(tags);
                        toggleAllTabs(false);
                        serviceStarted();
                        service.searchTweets(tags);
                    }
                }
            }
        });
        visualInsightsTable.addShortcutListener(new ShortcutListener("", ShortcutAction.KeyCode.ENTER, new int[0]) {
            @Override
            public void handleAction(Object sender, Object target) {
                Set<String> tags = visualInsightsTable.getConvertedValue();
                if (target == visualInsightsTable && !tags.isEmpty()) {
                    tweetList.searchStarted(tags);
                    toggleAllTabs(false);
                    serviceStarted();
                    service.searchTweets(tags);
                }
            }
        });


        insights = new TabSheet();
        insights.setStyleName(ValoTheme.TABSHEET_CENTERED_TABS);
        insights.setWidth("100%");



        TabSheet.Tab tab = insights.addTab(visualInsightsTable, "Visual Insights");
        tab.setEnabled(false);
        tab.setIcon(FontAwesome.LIST_ALT);
        tab = insights.addTab(visualInsightsChart, "Visual Insights chart");
        tab.setEnabled(false);
        tab.setIcon(FontAwesome.BAR_CHART_O);
        tab = insights.addTab(tweetList, "IBM Twitter Insights");
        tab.setEnabled(false);
        tab.setIcon(FontAwesome.TWITTER_SQUARE);

        MVerticalLayout l = new MVerticalLayout(messageLabel, wipSpinner).withMargin(false).withFullWidth().expand(insights);
        l.setHeightUndefined();
        return l;
    }

    private void toggleAllTabs(boolean enabled) {
        IntStream.range(0, insights.getComponentCount())
                .mapToObj(insights::getTab).forEach(t -> t.setEnabled(enabled));
    }

    private Layout infoPanel() {
        RichText info = ExternalLinkTarget.extend(new RichText()).withMarkDown(getClass().getResourceAsStream("insights.md"));
        gallery.addStyleName(ValoTheme.LAYOUT_HORIZONTAL_WRAPPING);

        toggleAllImages = new MButton(FontAwesome.CHECK_SQUARE_O)
                .withCaption("All/None");
        toggleAllImages.withListener(e -> {
            boolean newValue = !(boolean) toggleAllImages.getData();
            selectedResources.values().forEach(s -> s.setValue(newValue));
            onAllCardsWithSameStatus(newValue);
        }).withStyleName(ValoTheme.BUTTON_QUIET);
        toggleAllImages.setData(Boolean.FALSE);
        MHorizontalLayout buttons = new MHorizontalLayout(toggleAllImages, analyzeBtn)
                .withMargin(false);
        return new MVerticalLayout(info, buttons, gallery).withMargin(false).withAlign(buttons, Alignment.TOP_CENTER);
    }

    private void onAllCardsWithSameStatus(boolean newValue) {
        toggleAllImages.setData(newValue);
        toggleAllImages.setIcon((newValue) ? FontAwesome.SQUARE_O : FontAwesome.CHECK_SQUARE_O);
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
        getUI().scrollIntoView(insights);

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
        getUI().scrollIntoView(insights);
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
        getUI().scrollIntoView(wipSpinner);
    }

    private void serviceFailed(String message) {
        wipSpinner.setVisible(false);
        messageLabel.setStyleName(ValoTheme.LABEL_FAILURE);
        messageLabel.setValue(message);
        messageLabel.setVisible(true);
        analyzeBtn.setEnabled(hasSelectedCards());
        gallery.setEnabled(true);
        getUI().scrollIntoView(messageLabel);
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
        if (imageStorage.isEmpty()) {
            analyzeBtn.setEnabled(false);
        }
        hasAllCardSameStatus().ifPresent(InsightsView.this::onAllCardsWithSameStatus);
    }

    private Component galleryImage(ImageResource imageResource) {
        Image image = new Image(imageResource.getDescription(), imageResource.asVaadinResource());
        image.setAlternateText(imageResource.getDescription());
        image.setDescription(imageResource.getDescription());
        image.setWidth(100, Unit.PERCENTAGE);


        String btnStyles = Stream.of(ValoTheme.BUTTON_BORDERLESS, ValoTheme.BUTTON_ICON_ONLY, ValoTheme.BUTTON_SMALL)
                .collect(joining(" "));
        Label sizeLabel = new Label(imageResource.size());
        sizeLabel.addStyleName(ValoTheme.LABEL_TINY);
        sizeLabel.setWidth("100%");

        MHorizontalLayout actions = new MHorizontalLayout()
                .withSpacing(false).withMargin(false).withFullWidth().withStyleName("gallery-image-actions")
                .expand(sizeLabel)
                .add(
                        /*
                        new MButton(FontAwesome.TAGS, e -> Notification.show("Show tags")).withDescription("Visual recognition results")
                                .withStyleName(btnStyles),
                        new MButton(FontAwesome.TWITTER, e -> Notification.show("Start TwitterInsights")).withDescription("Search tweets")
                                .withStyleName(btnStyles),
                        */
                        new MButton(FontAwesome.TRASH_O, e -> destroyResource(imageResource)).withDescription("Delete image")
                                .withStyleName(btnStyles)
                ).alignAll(Alignment.MIDDLE_CENTER);


        MCssLayout card = new MCssLayout(image, actions).withStyleName("gallery-image");
        CardStatus status = selectedResources.computeIfAbsent(imageResource, k -> {
            CardStatus cs = new CardStatus();
            cs.addValueChangeListener(e -> {
                analyzeBtn.setEnabled(hasSelectedCards());
                hasAllCardSameStatus().ifPresent(InsightsView.this::onAllCardsWithSameStatus);
            });
            return cs;
        });
        Property.ValueChangeListener toggleCardStatusListener = e -> {
            card.setStyleName("selected", status.getValue());
        };
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

    private Optional<Boolean> hasAllCardSameStatus() {
        Set<Boolean> distinctStatus = selectedResources.values().stream().map(CardStatus::getValue)
                .distinct().collect(toSet());
        if (distinctStatus.size() == 1) {
            return Optional.of(distinctStatus.iterator().next());
        }
        return Optional.empty();
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
