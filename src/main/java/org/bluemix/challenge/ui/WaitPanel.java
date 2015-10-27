/* ====================================================================
 * Created on 26/10/15
 * Copyright (C) 2015 Insiel Mercato S.p.a.
 * <p>
 * org.bluemix.challenge.ui.WaitComponent
 * <p>
 * Comments are welcome.
 * <p>
 * - Marco Collovati <marco.collovati@insielmercato.it>
 * ====================================================================
 */
package org.bluemix.challenge.ui;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.ValoTheme;

import org.vaadin.spinkit.Spinner;
import org.vaadin.spinkit.SpinnerType;
import org.vaadin.viritin.label.RichText;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;

import java.util.Objects;


/**
 * @author Marco Collovati
 */
public class WaitPanel extends MHorizontalLayout {

    private final Spinner spinner = new Spinner(SpinnerType.WANDERING_CUBES);
    private final RichText info;
    private final String doneInfo;
    private final String startInfo;

    public WaitPanel(String startInfo, String doneInfo) {
        this.startInfo = Objects.requireNonNull(startInfo);
        this.doneInfo = Objects.requireNonNull(doneInfo);
        this.addStyleName(ValoTheme.LAYOUT_WELL);
        this.addStyleName("wait");
        this.info = new RichText().withMarkDown(getClass().getResourceAsStream(startInfo));
        this.info.setSizeFull();
        this.spinner.setVisible(false);
        expand(this.info, new MVerticalLayout(this.spinner)
                .withFullHeight().withFullWidth()
                .alignAll(Alignment.MIDDLE_CENTER)
        );
    }

    public WaitPanel start() {
        spinner.setVisible(true);
        return this;
    }

    public WaitPanel done() {
        this.info.withMarkDown(getClass().getResourceAsStream(doneInfo));
        this.spinner.setVisible(false);
        return this;
    }

    public WaitPanel info(String message) {
        Label messageLabel = createMessageLabel(message);
        messageLabel.addStyleName(ValoTheme.LABEL_COLORED);
        return this;
    }

    public WaitPanel error(String message) {
        createMessageLabel(message).setStyleName(ValoTheme.LABEL_FAILURE);
        return this;
    }

    private Label createMessageLabel(String message) {
        Objects.requireNonNull(message);
        Label label = new Label(message);
        label.setWidth(100, Unit.PERCENTAGE);
        ((MVerticalLayout)getComponent(1)).add(label);
        return label;
    }

}
