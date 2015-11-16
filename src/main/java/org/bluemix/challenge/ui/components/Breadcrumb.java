package org.bluemix.challenge.ui.components;

import com.vaadin.cdi.CDIView;
import com.vaadin.event.ShortcutAction;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

import org.bluemix.challenge.ui.DummyViews;
import org.bluemix.challenge.ui.RecognitionView;
import org.bluemix.challenge.ui.StartView;
import org.bluemix.challenge.ui.UploadView;
import org.vaadin.cdiviewmenu.ViewMenuItem;
import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.layouts.MCssLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by marco on 01/11/15.
 */
public class Breadcrumb extends MCssLayout implements ViewChangeListener {

    private static final String BUTTON_BREADCRUMB = "breadcrumb-btn";
    private AtomicInteger currentStep = new AtomicInteger();

    private final List<MButton> buttons = new ArrayList<>();
    private final Map<String, Integer> viewIndexes = new HashMap<>();
    private String currentView;

    public Breadcrumb() {
        withFullWidth().withStyleName("breadcrumb");
        addStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);
        addComponent(breadCrumbButton(StartView.class));
        addComponent(spacer());
        addComponent(breadCrumbButton(UploadView.class));
        addComponent(spacer());
        addComponent(breadCrumbButton(RecognitionView.class));
    }

    private MButton spacer() {
        MButton b = new MButton("");
        b.setPrimaryStyleName(BUTTON_BREADCRUMB);
        b.setEnabled(false);
        b.removeStyleName("v-disabled");
        b.addStyleName("spacer");
        return b;
    }
    private MButton breadCrumbButton(Class<? extends View> viewClazz) {
        ViewMenuItem menuItem = viewClazz.getAnnotation(ViewMenuItem.class);
        CDIView cdiView = viewClazz.getAnnotation(CDIView.class);
        MButton button = new MButton(menuItem.title()).withIcon(menuItem.icon());
                //.withListener(e -> UI.getCurrent().getNavigator().navigateTo(cdiView.value()));
        button.setPrimaryStyleName(BUTTON_BREADCRUMB);
        button.setData(cdiView.value());
        button.setEnabled(false);
        //button.setClickShortcut(ShortcutAction.KeyCode.NUM0 + buttons.size(), ShortcutAction.ModifierKey.ALT);
        viewIndexes.put(cdiView.value(), buttons.size());
        buttons.add(button);
        return button;
    }

    @Override
    public boolean beforeViewChange(ViewChangeEvent event) {
        Optional<Integer> viewIndex = Optional.ofNullable(viewIndexes.get(event.getViewName()));
        int currentViewIndex = Optional.ofNullable(currentView).map(viewIndexes::get).orElse(-1);
        boolean canChange = viewIndex.isPresent() && (viewIndex.get() <= currentViewIndex + 1);
        viewIndex.ifPresent( idx -> {
            toggleButtonsState(idx);
            if (event.getOldView() instanceof Component) {
                ((Component)event.getOldView()).removeStyleName("view-in");
                ((Component)event.getOldView()).addStyleName("view-out");
            }
        });

        return canChange;
    }


    @Override
    public void afterViewChange(ViewChangeEvent event) {
        currentView = event.getViewName();
        if (event.getNewView() instanceof Component) {
            Optional.ofNullable(event.getOldView())
                    .filter( o -> o instanceof Component)
                    .map( o -> (Component)o)
                    .ifPresent( c -> c.removeStyleName("view-out") );
            ((Component)event.getNewView()).addStyleName("view-in");
        }
    }

    private void toggleButtonsState(int currentViewIndex) {
        buttons.stream().limit(currentViewIndex).forEach( b -> {
            b.setEnabled(true);
            stylePrevious(b);
            findSpacer(b).ifPresent(Breadcrumb::stylePrevious);
        });
        MButton current = buttons.get(currentViewIndex);
        current.setEnabled(true);
        styleCurrent(current);
        findSpacer(current).ifPresent(Breadcrumb::styleCurrent);

        buttons.stream().skip(currentViewIndex+1).forEach(b -> {
            b.setEnabled(false);
            styleFollowing(b);
            findSpacer(b).ifPresent(Breadcrumb::styleFollowing);
        });
    }

    private Optional<Component> findSpacer(Component button) {
        int cidx = getComponentIndex(button) + 1;
        if (cidx < getComponentCount()) {
            return Optional.of(getComponent(cidx));
        }
        return Optional.empty();
    }
    private static void stylePrevious(Component component) {
        component.addStyleName(ValoTheme.BUTTON_PRIMARY);
        component.removeStyleName(ValoTheme.BUTTON_FRIENDLY);
    }
    private static void styleCurrent(Component component) {
        component.addStyleName(ValoTheme.BUTTON_FRIENDLY);
        component.removeStyleName(ValoTheme.BUTTON_PRIMARY);
    }
    private static void styleFollowing(Component component) {
        component.removeStyleName(ValoTheme.BUTTON_FRIENDLY);
        component.removeStyleName(ValoTheme.BUTTON_PRIMARY);
    }
}
