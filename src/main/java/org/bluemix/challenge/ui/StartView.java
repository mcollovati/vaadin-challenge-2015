/* ====================================================================
 * Created on 04/11/15
 * Copyright (C) 2015 Insiel Mercato S.p.a.
 * <p>
 * org.bluemix.challenge.ui.StartView
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
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.UI;

import org.vaadin.cdiviewmenu.ViewMenuItem;
import org.vaadin.viritin.button.PrimaryButton;
import org.vaadin.viritin.label.RichText;
import org.vaadin.viritin.layouts.MVerticalLayout;

/**
 * @author Marco Collovati
 */
@CDIView(value = StartView.VIEW_NAME)
@ViewMenuItem(order = 0, title = "Start", icon = FontAwesome.HOME)
public class StartView extends MVerticalLayout implements View{

    public static final String VIEW_NAME = "";

    public StartView() {
        withMargin(true).withFullWidth();
        setStyleName("start-view");
        addComponent(new RichText().withMarkDown(getClass().getResourceAsStream("start.md")));
        addComponent(new PrimaryButton("Clicke here to start",
                event -> UI.getCurrent().getNavigator().navigateTo(UploadView.VIEW_NAME)));
        expand(new CssLayout());
        setHeightUndefined();

    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {

    }
}
