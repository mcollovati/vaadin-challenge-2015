package org.bluemix.challenge.ui.components;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;
import com.vaadin.ui.themes.ValoTheme;
import lombok.Getter;
import lombok.Setter;
import org.vaadin.spinkit.Spinner;
import org.vaadin.spinkit.SpinnerType;
import org.vaadin.viritin.fields.MTable;
import org.vaadin.viritin.layouts.MMarginInfo;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.watson.twitterinsights.response.Tweet;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;

/**
 * @author Marco Collovati
 */
public class TweetList extends MVerticalLayout { //extends Panel {

    private static final Comparator<Tweet> TWEET_COMPARATOR =
            Comparator.comparingLong( (Tweet t) -> t.getMessage().getPostedTime().getTime() )
                .reversed();
    ////MVerticalLayout content = new MVerticalLayout().withMargin(new MMarginInfo(true,false)).withFullWidth();

    BeanItemContainer<Tweet> tweetsContainer = new BeanItemContainer<Tweet>(Tweet.class);
    MTable<Tweet> tweetsTable = new MTable<>(Tweet.class).withFullWidth();
    Label query = new Label();

    @Getter
    private int tweetLimit = 10;


    public TweetList() {
        addStyleName("twitter-insights-results");
        MVerticalLayout content = this.withFullWidth().withMargin(false);

        setSizeFull();
        addStyleName("tweet-list");
        content.setDefaultComponentAlignment(Alignment.TOP_CENTER);
        ////setContent(content);
        ////setCaption("IBM Insights for Twitter");
        initTweetGrid();

        query.addStyleName(ValoTheme.LABEL_BOLD);
        query.addStyleName(ValoTheme.LABEL_COLORED);

        content.add(query,tweetsTable);
    }

    public void setTweetLimit(int tweetLimit) {
        if (tweetLimit <= 0) {
            throw new IllegalArgumentException("Tweet limit must be > 0");
        }
        this.tweetLimit = tweetLimit;
    }

    private void initTweetGrid() {
        tweetsTable.setContainerDataSource(tweetsContainer);
        tweetsTable.setColumnHeaderMode(Table.ColumnHeaderMode.HIDDEN);
        tweetsTable.setFooterVisible(false);
        tweetsTable.addGeneratedColumn("body", (Table.ColumnGenerator) (source, itemId, columnId) -> new TweetComponent((Tweet)itemId).withMargin(false));
        tweetsTable.setVisibleColumns("body");
        tweetsTable.setPageLength(0);
        ////tweetsTable.setHeight("100%");

    }

    public void searchStarted(Set<String> tags)  {
        query.setValue(String.format("Tweets containing keywords: %s. (Only first %d are shown)", String.join(", ", tags), tweetLimit));
        //content.removeAllComponents();
        //content.add(new Spinner(SpinnerType.THREE_BOUNCE));
        //setContent(new Spinner(SpinnerType.THREE_BOUNCE));
    }

    public void setTweets(List<Tweet> tweets) {
        tweetsContainer.removeAllItems();
        tweetsContainer.addAll(tweets.stream().sorted(TWEET_COMPARATOR).limit(tweetLimit).collect(toList()));
        setVisible(!tweets.isEmpty());
    }

    public void empty() {
        setTweets(Collections.emptyList());
    }
}
