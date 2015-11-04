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
import com.vaadin.ui.Label;

import org.vaadin.cdiviewmenu.ViewMenuItem;
import org.vaadin.viritin.label.RichText;

/**
 * @author Marco Collovati
 */
@CDIView(value = StartView.VIEW_NAME)
@ViewMenuItem(order = 3, title = "Last view", icon = FontAwesome.OUTDENT)
public class StartView extends CssLayout implements View{

    public static final String VIEW_NAME = "vv";
    public StartView() {
        setSizeFull();
        addComponent(new Label("a view"));
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {

    }
}
