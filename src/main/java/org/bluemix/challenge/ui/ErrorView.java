package org.bluemix.challenge.ui;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.label.RichText;
import org.vaadin.viritin.layouts.MCssLayout;

import javax.annotation.PostConstruct;

/**
 * Created by marco on 01/11/15.
 */
public class ErrorView extends MCssLayout implements View {

    public ErrorView() {
        setStyleName("error-view");
        setSizeFull();
        Label label = new Label(FontAwesome.MEH_O.getHtml());
        label.setStyleName(ValoTheme.LABEL_H1);
        addComponent(new RichText().withMarkDown(getClass().getResourceAsStream("error.md")));
        addComponent(new MButton(FontAwesome.ARROW_LEFT)
                .withCaption("Go back to image upload page")
                .withStyleName(String.join(" ", ValoTheme.BUTTON_HUGE, ValoTheme.BUTTON_FRIENDLY))
                .withListener( e -> UI.getCurrent().getNavigator().navigateTo(UploadView.VIEW_NAME))
        );
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {

    }
}
