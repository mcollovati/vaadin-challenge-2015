package org.bluemix.challenge.ui.components;

import com.vaadin.cdi.CDIView;
import com.vaadin.cdi.UIScoped;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

import org.bluemix.challenge.io.ImageStorage;
import org.bluemix.challenge.ui.*;
import org.vaadin.cdiviewmenu.ViewMenuItem;
import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.layouts.MCssLayout;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**
 * Created by marco on 01/11/15.
 */
@UIScoped
public class Breadcrumb extends MCssLayout implements ViewChangeListener {

    private static final String BUTTON_BREADCRUMB = "breadcrumb-btn";

    private final Map<String, Predicate<ViewChangeEvent>> permissions = new HashMap<>();

    private final List<MButton> buttons = new ArrayList<>();
    private final Map<String, Integer> viewIndexes = new HashMap<>();

    private String currentView;

    @Inject
    public Breadcrumb(ImageStorage imageStorage) {
        withFullWidth().withStyleName("breadcrumb");
        addStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);
        addComponent(breadCrumbButton(StartView.class));
        addComponent(spacer());
        addComponent(breadCrumbButton(UploadView.class));
        addComponent(spacer());
        addComponent(breadCrumbButton(RecognitionView.class, false, ev -> RecognitionView.VIEW_NAME.equals(ev.getViewName()) && ev.getOldView() instanceof UploadView));
        addComponent(spacer());
        addComponent(breadCrumbButton(InsightsView.class, true, ev -> !imageStorage.isEmpty()));
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
        return breadCrumbButton(viewClazz, true);
    }
    private MButton breadCrumbButton(Class<? extends View> viewClazz, boolean linked) {
        return breadCrumbButton(viewClazz, linked, e -> true);
    }
    private MButton breadCrumbButton(Class<? extends View> viewClazz, boolean linked, Predicate<ViewChangeEvent> permission) {
        ViewMenuItem menuItem = viewClazz.getAnnotation(ViewMenuItem.class);
        CDIView cdiView = viewClazz.getAnnotation(CDIView.class);
        MButton button = new MButton(menuItem.title()).withIcon(menuItem.icon())
                .withDescription(menuItem.title());
        if (linked) {
            button.withListener(e -> UI.getCurrent().getNavigator().navigateTo(cdiView.value()));
        }
        button.setPrimaryStyleName(BUTTON_BREADCRUMB);
        //button.addStyleName(ValoTheme.BUTTON_BORDERLESS);
        button.setData(cdiView.value());
        button.setEnabled(false);
        //button.setClickShortcut(ShortcutAction.KeyCode.NUM0 + buttons.size(), ShortcutAction.ModifierKey.ALT);
        viewIndexes.put(cdiView.value(), buttons.size());
        buttons.add(button);
        permissions.put(cdiView.value(), permission);
        return button;
    }

    @Override
    public boolean beforeViewChange(ViewChangeEvent event) {
        Optional<Integer> viewIndex = Optional.ofNullable(viewIndexes.get(event.getViewName()));
        /*
        int currentViewIndex = Optional.ofNullable(currentView).map(viewIndexes::get).orElse(-1);
        boolean canChange = viewIndex.isPresent() && (viewIndex.get() <= currentViewIndex + 1);
         */
        boolean canChange = viewIndex.isPresent() && Optional.ofNullable(permissions.get(event.getViewName()))
                .map( p -> p.test(event)).orElse(false);
        viewIndex.ifPresent( idx -> {

            toggleButtonsState(idx, event);
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

    private void toggleButtonsState(int currentViewIndex, ViewChangeEvent event) {
        buttons.stream().forEach( b -> {
            b.removeStyleName(ValoTheme.BUTTON_FRIENDLY);
        });
        buttons.stream().limit(currentViewIndex).forEach( b -> {
            b.setEnabled(permissions.get(b.getData()).test(event));
        });
        MButton current = buttons.get(currentViewIndex);
        current.setEnabled(true);
        current.addStyleName(ValoTheme.BUTTON_FRIENDLY);

        buttons.stream().skip(currentViewIndex+1).forEach(b -> {
            b.setEnabled(permissions.get(b.getData()).test(event));
        });
    }


}
