package org.bluemix.challenge.ui.components;

import com.vaadin.addon.charts.Chart;
import com.vaadin.addon.charts.PointClickEvent;
import com.vaadin.addon.charts.PointClickListener;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Notification;
import org.watson.visualinsights.response.Summary;

import java.util.Collections;
import java.util.List;

/**
 * Created by marco on 19/11/15.
 */
public class VisualInsightsChart extends CssLayout {

    private Chart chart;

    public VisualInsightsChart() {
        addStyleName("visual-insights-chart");
        chart = VisualInsightsHelper.drawChart(Collections.emptyList());
        addComponent(chart);
    }

    public void empty() {
        withSummaryList(Collections.emptyList());
    }

    public void withSummaryList(List<Summary> data) {
        Chart newChart = VisualInsightsHelper.drawChart(data);
        replaceComponent(chart, chart = newChart);
        setVisible(!data.isEmpty());
    }
}
