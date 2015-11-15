/* ====================================================================
 * Created on 07/11/15
 * Copyright (C) 2015 Insiel Mercato S.p.a.
 * <p>
 * org.bluemix.challenge.ui.components.TweetComponent
 * <p>
 * Comments are welcome.
 * <p>
 * - Marco Collovati <marco.collovati@insielmercato.it>
 * ====================================================================
 */
package org.bluemix.challenge.ui.components;

import com.vaadin.server.ExternalResource;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.themes.ValoTheme;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.ocpsoft.prettytime.PrettyTime;
import org.vaadin.viritin.label.RichText;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MMarginInfo;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.watson.twitterinsights.response.Actor;
import org.watson.twitterinsights.response.Hashtag;
import org.watson.twitterinsights.response.Message;
import org.watson.twitterinsights.response.Tweet;
import org.watson.twitterinsights.response.Url;
import org.watson.twitterinsights.response.UserMention;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author Marco Collovati
 */
public class TweetComponent extends MVerticalLayout {

    TweetComponent(Tweet tweet) {
        addStyleName("tweet");
        addStyleName(ValoTheme.PANEL_BORDERLESS);
        withFullWidth()
                .withSpacing(false)
                .withMargin(new MarginInfo(false, true, false, true));


        Actor actor = tweet.getMessage().getActor();
        Image authorImage = new Image();
        authorImage.addStyleName("tweet-author-image");
        authorImage.setSource(new ExternalResource(actor.getImage()));

        Label name = new Label(actor.getDisplayName());
        name.setStyleName("tweet-author-name");
        name.addStyleName(ValoTheme.LABEL_BOLD);

        Link authorLink = new Link("@" + actor.getPreferredUsername(),
                new ExternalResource(actor.getLink()));

        RichText body = new RichText().withMarkDown(processTweetBody(tweet.getMessage()));
        body.setStyleName("tweet-body");
        body.setWidth("100%");
        body.setHeightUndefined();




        Label tweetDate = new Label(new PrettyTime(new Date()).format(tweet.getMessage().getPostedTime()));
                /*String.format("%s on %s at %s", actor.getDisplayName(),
                        DateFormatUtils.format(tweet.getMessage().getPostedTime(), "yyyy-MM-dd"),
                        DateFormatUtils.format(tweet.getMessage().getPostedTime(), "HH:mm")
                ));*/

        tweetDate.addStyleName(ValoTheme.LABEL_BOLD);

        MVerticalLayout authorBox = new MVerticalLayout(name, authorLink)
                .withMargin(false).withSpacing(false)
                .withFullWidth();
        add(tweetDate, body, new MHorizontalLayout(authorImage)
                .withMargin(false).withSpacing(true).withFullWidth()
                .expand(authorBox)
                .withAlign(authorBox, Alignment.MIDDLE_LEFT)
        );

        /*
        add(new MVerticalLayout(
                        authorImage,
                        authorLink
                ).withWidth("120px").withMargin(false).withSpacing(false)
                        .withAlign(authorImage, Alignment.TOP_CENTER)
                        .withAlign(authorLink, Alignment.TOP_CENTER)
        ).expand(new MVerticalLayout(tweetDate, body).withMargin(new MarginInfo(false, true, false, true))
                .withSpacing(false).withFullWidth());
                */
    }

    private String processTweetBody(Message message) {
        String originalBody = message.getBody();
        StringBuilder body = new StringBuilder();

        TreeMap<Index, Object> map = new TreeMap<>();
        for (Url url : message.getTwitterEntities().getUrls()) {
            for (Index idx : Index.build(url.getIndices())) {
                map.put(idx, url);
            }
        }
        for (Hashtag hashtag : message.getTwitterEntities().getHashtags()) {
            for (Index idx : Index.build(hashtag.getIndices())) {
                map.put(idx, hashtag);
            }
        }
        for (UserMention userMention : message.getTwitterEntities().getUserMentions()) {
            for (Index idx : Index.build(userMention.getIndices())) {
                map.put(idx, userMention);
            }
        }

        System.out.println("ORIGINAL: " + originalBody);
        int lastIndex = 0;
        for (Map.Entry<Index, Object> o : map.entrySet()) {
            Index idx = o.getKey();
            if (lastIndex < idx.from) {
                Object val = o.getValue();
                body.append(originalBody.substring(lastIndex, idx.from));
                if (val instanceof Url) {
                    Url url = (Url) val;
                    appendUrl(body, url.getDisplayUrl(), url.getUrl());
                } else if (val instanceof Hashtag) {
                    Hashtag hashtag = (Hashtag) val;
                    appendUrl(body, String.format("#%s", hashtag.getText()), String.format("https://twitter.com/hashtag/%s?src=hash", hashtag.getText()));
                } else if (val instanceof UserMention) {
                    UserMention mention = (UserMention) val;
                    appendUrl(body, String.format("@%s", mention.getScreenName()), String.format("https://twitter.com/%s", mention.getScreenName()));
                } else {
                    body.append(originalBody.substring(idx.from, idx.to));
                }
                lastIndex = idx.to;
                System.out.println("After index " + idx.toString() + ": " + body.toString());
            } else {
                System.out.println("Maybe invalid index " + idx + ". Last processed index was " + lastIndex);
            }
        }
        body.append(originalBody.substring(lastIndex));
        System.out.println("Result: " + body.toString());
        return body.toString();
    }


    void appendUrl(StringBuilder body, String text, String url) {
        body.append("[").append(text).append("](")
                .append(url).append(")");

    }

    private static class Index implements Comparable<Index> {
        final int from, to;

        public static Set<Index> build(List<Long> indexes) {
            LinkedHashSet<Index> idxs = new LinkedHashSet<>();
            Iterator<Long> iter = indexes.iterator();
            while (iter.hasNext()) {
                int from = iter.next().intValue();
                if (iter.hasNext()) {
                    idxs.add(new Index(from, iter.next().intValue()));
                }
            }
            return idxs;
        }

        public Index(int from, int to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public int compareTo(Index o) {
            int res = Long.compare(from, o.from);
            if (res == 0) {
                return Long.compare(to, o.to);
            }
            return res;
        }

        @Override
        public String toString() {
            return String.format("[%d, %d]", from, to);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Index index = (Index) o;
            return from == index.from &&
                    to == index.to;
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to);
        }
    }

}
