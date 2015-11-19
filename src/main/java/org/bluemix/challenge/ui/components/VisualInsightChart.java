package org.bluemix.challenge.ui.components;

import com.vaadin.addon.charts.Chart;
import com.vaadin.addon.charts.model.*;
import com.vaadin.addon.charts.model.style.Color;
import com.vaadin.addon.charts.model.style.PlotOptionsStyle;
import com.vaadin.addon.charts.model.style.SolidColor;
import com.vaadin.addon.charts.themes.ValoLightTheme;
import lombok.extern.slf4j.Slf4j;
import org.watson.visualinsights.response.Summary;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.IntStream;

/**
 * Created by marco on 19/11/15.
 */
@Slf4j
public class VisualInsightChart {

    private static Random rand = new Random(0);
    private static final ValoLightTheme theme = new ValoLightTheme();
    private static final Color[] colors = theme.getColors();
    private final static BigDecimal HUNDRED = new BigDecimal(100);


    public static Chart draw(List<Summary> data) {
        Chart chart = new Chart(ChartType.PIE);
        Configuration conf = chart.getConfiguration();
        conf.setTitle("Tagging Profile");

        /*
        YAxis yaxis = new YAxis();
        yaxis.setTitle("Total percent market share");
        */

        PlotOptionsPie pie = new PlotOptionsPie();
        pie.setShadow(false);
        conf.setPlotOptions(pie);

        //conf.getTooltip().setValueSuffix("%");
        prepareSeries(data, chart);
        return chart;
    }

    private static void prepareSeries(List<Summary> data, Chart chart) {
        List<DataSeries> dataSeries = new ArrayList<>();
        DataSeries inner = new DataSeries("");
        PlotOptionsPie innerPieOptions= new PlotOptionsPie();
        innerPieOptions.setSize(240);
        inner.setPlotOptions(innerPieOptions);
        innerPieOptions.setDataLabels(new Labels());
        innerPieOptions.getDataLabels().setFormatter(
                "this.y > 5 ? this.point.name : null");
        innerPieOptions.getDataLabels().setColor(new SolidColor(255, 255, 255));
        innerPieOptions.getDataLabels().setDistance(-30);


        dataSeries.add(inner);
        DataSeries outer = new DataSeries("");
        PlotOptionsPie outerPierOptions = new PlotOptionsPie();
        outerPierOptions.setSize(280);
        outerPierOptions.setInnerSize(240);
        outerPierOptions.setDataLabels(new Labels());
        outerPierOptions.getDataLabels()
                .setFormatter("this.y > 1 ? ''+ this.point.name +': '+ this.y  : null");

        outer.setPlotOptions(outerPierOptions);
        dataSeries.add(outer);

        data.stream()
                .filter(s -> BigDecimal.ZERO.compareTo(s.getScore()) < 0)
                .sorted(Comparator.comparing(Summary::getName))
                .map(s -> {
                    Summary newS = new Summary();
                    newS.setName(s.getName());
                    newS.setScore(s.getScore().multiply(HUNDRED).setScale(2, BigDecimal.ROUND_HALF_EVEN));
                    return newS;
                })
                .peek(s -> log.debug(s.toString()))
                .forEach(s -> asDataSeries(s, dataSeries));
        chart.getConfiguration().setSeries(dataSeries.stream().toArray(Series[]::new));
        chart.drawChart();
    }


    private static void asDataSeries(Summary s, List<DataSeries> dataSeries) {
        String[] path = s.getName().split("/");

        DataSeries inner = dataSeries.get(0);
        DataSeriesItem innerItem = inner.get(path[0]);
        if (innerItem == null) {
            innerItem = new DataSeriesItem(path[0], BigDecimal.ZERO, colors[inner.size()]);
            inner.add(innerItem);
        }
        innerItem.setY( ((BigDecimal)innerItem.getY()).add(s.getScore()) );

        DataSeriesItem outerItem = new DataSeriesItem(path[path.length - 1], s.getScore(), color(innerItem.getColor()));
        dataSeries.get(1).add(outerItem);
    }

    private static Color color(Color baseColor) {
        SolidColor c = (SolidColor) baseColor;
        String cStr = c.toString().substring(1);

        int r = Integer.parseInt(cStr.substring(0, 2), 16);
        int g = Integer.parseInt(cStr.substring(2, 4), 16);
        int b = Integer.parseInt(cStr.substring(4, 6), 16);


        double opacity = (50 + rand.nextInt(95 - 50)) / 100.0;

        return new SolidColor(r, g, b, opacity);

    }

}
