package org.bluemix.challenge.ui.components;

import com.google.common.base.Strings;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.ui.TreeTable;
import org.watson.visualinsights.response.Summary;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

/**
 * Created by marco on 20/11/15.
 */
public class VisualInsightsTable extends TreeTable {

    //private static final String PATH_PROPERTY = "path";
    private static final String NAME_PROPERTY = "name";
    private static final String SCORE_PROPERTY = "score";

    public VisualInsightsTable() {
        setSelectable(true);
        setMultiSelect(true);

        //addContainerProperty(PATH_PROPERTY, String.class,"");
        addContainerProperty(NAME_PROPERTY, String.class, "");
        addContainerProperty(SCORE_PROPERTY, BigDecimal.class, BigDecimal.ZERO);

        setVisibleColumns(NAME_PROPERTY, SCORE_PROPERTY);
        setColumnHeaders("Label", "Score");
        setColumnExpandRatio(NAME_PROPERTY, 1);
        setColumnReorderingAllowed(false);
        setSortContainerPropertyId(SCORE_PROPERTY);
        setSortAscending(false);

    }

    @Override
    public Object getConvertedValue() {
        return Optional.ofNullable(getValue())
                .map(v -> (Set<String>) v)
                .map(set -> set.stream().map(tag -> Stream.of(tag.split("/")).reduce("", (s1, s2) -> s2)).collect(toSet()))
                .orElse(null);
    }

    public void empty() {
        removeAllItems();
    }

    public void withSummaryList(List<Summary> data) {
        empty();
        VisualInsightsHelper.filterAndSort(data.stream())
                .forEach(this::addTreeItem);
        expandAll();
        setVisible(!data.isEmpty());
        sort(new Object[] { SCORE_PROPERTY}, new boolean[] { false });
    }

    private void expandAll() {

        getContainerDataSource().getItemIds().stream()
                .map(o -> (String) o)
                .sorted(Comparator.comparing(s -> s.chars().filter(ch -> '/' == ch).count()))
                //.peek(System.out::println)
                .forEach(itemId -> setCollapsed(itemId, false));
    }

    private void addTreeItem(Summary summary) {
        String[] path = summary.getName().split("/");
        String parentItemId = "";
        for (int i = 0; i < path.length; i++) {
            String segment = path[i];
            //itemId = String.join("/", itemId, segment);
            final String itemId = String.join("/", parentItemId, segment);
            Item item = Optional.ofNullable(getItem(itemId)).orElseGet(() -> {
                addItem(new Object[]{segment, BigDecimal.ZERO}, itemId);
                return getItem(itemId);
            });
            Property<BigDecimal> prop = item.getItemProperty(SCORE_PROPERTY);
            prop.setValue(prop.getValue().add(summary.getScore()));
            setParent(itemId, Strings.emptyToNull(parentItemId));
            setChildrenAllowed(itemId, i < path.length - 1);

            parentItemId = itemId;
        }
    }
}
