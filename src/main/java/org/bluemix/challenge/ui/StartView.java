package org.bluemix.challenge.ui;

import com.vaadin.cdi.CDIView;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.UI;
import org.bluemix.challenge.ui.components.ExternalLinkTarget;
import org.vaadin.cdiviewmenu.ViewMenuItem;
import org.vaadin.viritin.button.PrimaryButton;
import org.vaadin.viritin.label.RichText;
import org.vaadin.viritin.layouts.MVerticalLayout;

/**
 * @author Marco Collovati
 */
@CDIView(value = StartView.VIEW_NAME)
@ViewMenuItem(order = 0, title = "Start", icon = FontAwesome.HOME)
public class StartView extends MVerticalLayout implements View {

    public static final String VIEW_NAME = "";

    public StartView() {
        withMargin(true).withFullWidth();
        setStyleName("start-view two-columns");
        addComponent(new RichText().withMarkDown(getClass().getResourceAsStream("start.md")));
        addComponent(new PrimaryButton("Start here",
                event -> UI.getCurrent().getNavigator().navigateTo(UploadView.VIEW_NAME)));
        addComponent(ExternalLinkTarget.extend(new RichText()).withMarkDown(getClass().getResourceAsStream("services.md")));
        expand(new CssLayout());
        setHeightUndefined();

    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {

    }
}
