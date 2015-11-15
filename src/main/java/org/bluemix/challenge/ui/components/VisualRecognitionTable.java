package org.bluemix.challenge.ui.components;

import com.vaadin.data.Item;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.util.GeneratedPropertyContainer;
import com.vaadin.data.util.PropertyValueGenerator;
import com.vaadin.data.util.converter.AbstractStringToNumberConverter;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.data.util.converter.StringToIntegerConverter;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.grid.HeightMode;
import com.vaadin.ui.Grid;
import com.vaadin.ui.renderers.HtmlRenderer;
import com.vaadin.ui.renderers.NumberRenderer;

import org.vaadin.viritin.fields.MTable;
import org.watson.visualrecognition.response.Image;
import org.watson.visualrecognition.response.Label;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by marco on 20/10/15.
 */
public class VisualRecognitionTable extends Grid {

    BeanItemContainer<Label> container = new BeanItemContainer<>(Label.class);

    public VisualRecognitionTable() {

        GeneratedPropertyContainer containerWrapper = new GeneratedPropertyContainer(container);
        containerWrapper.addGeneratedProperty("labelStars", new PropertyValueGenerator<Integer>() {
            @Override
            public Integer getValue(Item item, Object itemId, Object propertyId) {
                return new Double(Math.round(((Label) itemId).getLabelScore() * 10)).intValue();
            }

            @Override
            public Class<Integer> getType() {
                return Integer.class;
            }
        });
        setContainerDataSource(containerWrapper);

        setColumns("labelName", "labelScore", "labelStars");
        getColumn("labelName").setHeaderCaption("Label").setExpandRatio(1);
        getColumn("labelScore").setHeaderCaption("Score")
                .setRenderer(new NumberRenderer("%.2f"));
        getColumn("labelStars").setHeaderCaption("")
                .setRenderer(new HtmlRenderer(), new StringToIntegerConverter() {
                    @Override
                    public String convertToPresentation(Integer value, Class<? extends String> targetType, Locale locale) throws ConversionException {

                        return IntStream.range(0, 5).mapToObj(i -> (i <= value / 2) ? FontAwesome.STAR : FontAwesome.STAR_O)
                                .map(FontAwesome::getHtml)
                                .reduce(String::concat).orElse("");
                    }
                });
        setFooterVisible(false);
        setColumnReorderingAllowed(false);
        //withProperties("labelName", "labelScore");
        //withColumnHeaders("Label", "Score");
        //setSelectable(true);
        setSelectionMode(SelectionMode.SINGLE);
        setImmediate(true);
    }

    public void withImageResponse(Image response) {
        List<Label> labels = response.getLabels();
        if (labels == null) {
            labels = new ArrayList<>();
        }
        container.removeAllItems();
        container.addAll(labels);
        //setBeans(labels);
        markAsDirty();
    }

    public void clearTable() {
        container.removeAllItems();
        //super.setBeans();
    }

    public Label getValue() {
        return (Label) getSelectedRow();
    }
}
