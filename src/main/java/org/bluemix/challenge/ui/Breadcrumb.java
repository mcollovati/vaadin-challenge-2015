package org.bluemix.challenge.ui;

import com.vaadin.cdi.CDIView;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

import org.vaadin.cdiviewmenu.ViewMenuItem;
import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.layouts.MCssLayout;
import org.vaadin.viritin.layouts.MHorizontalLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by marco on 01/11/15.
 */
public class Breadcrumb extends MCssLayout implements ViewChangeListener{

    private final List<MButton> buttons = new ArrayList<>();

    public Breadcrumb() {
        withFullWidth().withStyleName("breadcrumb");
        addStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);
        addComponent(breadCrumbButton(UploadView.class));
        addComponent(breadCrumbButton(RecognitionView.class));
    }

    private MButton breadCrumbButton(Class<? extends View> viewClazz) {
        ViewMenuItem menuItem = viewClazz.getAnnotation(ViewMenuItem.class);
        CDIView cdiView = viewClazz.getAnnotation(CDIView.class);
        MButton button = new MButton(menuItem.title()).withIcon(menuItem.icon())
                .withStyleName("breadcrumb-btn").withVisible(true)
        .withListener( e -> UI.getCurrent().getNavigator().navigateTo(cdiView.value()));
        buttons.add(button);
        return button;
    }

    @Override
    public boolean beforeViewChange(ViewChangeEvent event) {
        return true;
    }

    @Override
    public void afterViewChange(ViewChangeEvent event) {

    }
}
