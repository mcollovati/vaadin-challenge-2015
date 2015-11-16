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
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Layout;
import com.vaadin.ui.themes.ValoTheme;
import lombok.extern.slf4j.Slf4j;
import org.bluemix.challenge.io.ImageResource;
import org.bluemix.challenge.io.ImageStorage;
import org.bluemix.challenge.ui.components.TweetList;
import org.vaadin.cdiviewmenu.ViewMenuItem;
import org.vaadin.viritin.label.RichText;
import org.vaadin.viritin.layouts.MCssLayout;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;

import javax.annotation.PostConstruct;
import javax.inject.Inject;


/**
 * @author Marco Collovati
 */
@Slf4j
@CDIView(InsightsView.VIEW_NAME)
@ViewMenuItem(title = "Insights", icon = FontAwesome.IMAGE, order = 4)
public class InsightsView extends MHorizontalLayout implements View {

    public static final String VIEW_NAME = "insights";

    @Inject
    private ImageStorage imageStorage;

    private MCssLayout gallery = new MCssLayout().withStyleName("gallery")
            .withFullWidth();
    private final TweetList tweetList = new TweetList();


    @PostConstruct
    void initComponents() {
        withMargin(true).withFullWidth();
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
        return new MVerticalLayout(info, gallery);
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        gallery.removeAllComponents();
        imageStorage.getResources().stream()
                .map(this::galleryImage)
                .forEach(c -> {
                    gallery.addComponent(c);
                    //gallery.setExpandRatio(c, 0.3f);
                });
    }

    private Component galleryImage(ImageResource imageResource) {
        Image image = new Image("", imageResource.asVaadinResource());
        image.setWidth(100,Unit.PERCENTAGE);
        return new MCssLayout(image).withStyleName("gallery-image");
    }
}
