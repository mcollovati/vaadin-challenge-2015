/* ====================================================================
 * Created on 07/11/15
 * Copyright (C) 2015 Insiel Mercato S.p.a.
 * <p>
 * org.bluemix.challenge.ui.components.TweetList
 * <p>
 * Comments are welcome.
 * <p>
 * - Marco Collovati <marco.collovati@insielmercato.it>
 * ====================================================================
 */
package org.bluemix.challenge.ui.components;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Panel;
import com.vaadin.ui.renderers.ImageRenderer;

import org.vaadin.spinkit.Spinner;
import org.vaadin.spinkit.SpinnerType;
import org.vaadin.viritin.layouts.MMarginInfo;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.watson.twitterinsights.response.Tweet;

import java.util.Comparator;
import java.util.List;

/**
 * @author Marco Collovati
 */
public class TweetList extends Panel {

    private static final Comparator<Tweet> TWEET_COMPARATOR =
            Comparator.comparingLong( (Tweet t) -> t.getMessage().getPostedTime().getTime() )
                .reversed();
    MVerticalLayout content = new MVerticalLayout().withMargin(new MMarginInfo(true,false)).withFullWidth();

    public TweetList() {
        setSizeFull();
        content.setDefaultComponentAlignment(Alignment.TOP_CENTER);
        setContent(content);
        setCaption("IBM Insights for Twitter");
        //withMargin(false).withFullWidth();
    }

    public void searchStarted() {
        content.removeAllComponents();
        content.add(new Spinner(SpinnerType.THREE_BOUNCE));
    }

    public void setTweets(List<Tweet> tweets) {
        content.removeAllComponents();
        tweets.stream().sorted(TWEET_COMPARATOR)
                .map(TweetComponent::new)
                .forEach(content::addComponent);
    }

}
