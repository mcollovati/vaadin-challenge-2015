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

import com.vaadin.client.widget.grid.datasources.ListDataSource;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;

import org.vaadin.spinkit.Spinner;
import org.vaadin.spinkit.SpinnerType;
import org.vaadin.viritin.LazyList;
import org.vaadin.viritin.fields.MTable;
import org.vaadin.viritin.layouts.MMarginInfo;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.watson.twitterinsights.response.Tweet;

import java.util.Comparator;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * @author Marco Collovati
 */
public class TweetList extends Panel {

    private static final Comparator<Tweet> TWEET_COMPARATOR =
            Comparator.comparingLong( (Tweet t) -> t.getMessage().getPostedTime().getTime() )
                .reversed();
    MVerticalLayout content = new MVerticalLayout().withMargin(new MMarginInfo(true,false)).withFullWidth();

    BeanItemContainer<Tweet> tweetsContainer = new BeanItemContainer<Tweet>(Tweet.class);
    MTable<Tweet> tweetsTable = new MTable<>();


    public TweetList() {
        setSizeFull();
        addStyleName("tweet-list");
        content.setDefaultComponentAlignment(Alignment.TOP_CENTER);
        setContent(content);
        setCaption("IBM Insights for Twitter");
        initTweetGrid();

    }

    private void initTweetGrid() {
        tweetsTable.setContainerDataSource(tweetsContainer);
        tweetsTable.setColumnHeaderMode(Table.ColumnHeaderMode.HIDDEN);
        tweetsTable.setFooterVisible(false);
        tweetsTable.addGeneratedColumn("body", (Table.ColumnGenerator) (source, itemId, columnId) -> new TweetComponent((Tweet)itemId).withMargin(false));
        tweetsTable.setVisibleColumns("body");
    }

    public void searchStarted() {
        //content.removeAllComponents();
        //content.add(new Spinner(SpinnerType.THREE_BOUNCE));
        setContent(new Spinner(SpinnerType.THREE_BOUNCE));
    }

    public void setTweets(List<Tweet> tweets) {
        tweetsContainer.removeAllItems();
        tweetsContainer.addAll(tweets.stream().sorted(TWEET_COMPARATOR).collect(toList()));
        setContent(tweetsTable);
        /*
        content.removeAllComponents();
        tweets.stream().sorted(TWEET_COMPARATOR)
                .map(TweetComponent::new)
                .forEach(content::addComponent);
                */
    }

}
