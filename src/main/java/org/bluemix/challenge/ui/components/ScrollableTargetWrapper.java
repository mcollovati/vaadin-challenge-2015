package org.bluemix.challenge.ui.components;

import com.vaadin.server.Page;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.CustomComponent;
import org.vaadin.viritin.layouts.MCssLayout;

import java.util.Optional;

/**
 * Created by marco on 21/11/15.
 */

public class ScrollableTargetWrapper<T extends Component> extends CustomComponent {

    private final CssLayout anchor = new MCssLayout().withHeight("0px").withFullWidth();
    private final T wrapped;

    public ScrollableTargetWrapper(T wrapped) {
        setWidth(100, Unit.PERCENTAGE);
        setPrimaryStyleName("scrollable-target-wrapper");
        this.wrapped = wrapped;
        CssLayout root = new CssLayout(anchor, wrapped);
        root.setWidth(100, Unit.PERCENTAGE);
        setCompositionRoot(root);
    }

    public T getTarget() {
        return wrapped;
    }

    public void scroll() {
        //getUI().scrollIntoView(wrapped);
        getUI().scrollIntoView(anchor);
        if (anchor instanceof Focusable) {
            ((Focusable)anchor).focus();
        }
    }

    @Override
    public void attach() {
        Page.getCurrent().getJavaScript().addFunction("org.bluemix.challenge.scrollTo", arguments -> {

        });
        super.attach();
    }

    public static void scrollTo(Component component) {
        wrapperFor(component).ifPresent(ScrollableTargetWrapper::scroll);
    }

    public static Optional<ScrollableTargetWrapper> wrapperFor(Component component) {
        Component parent = component;
        while (parent != null && !ScrollableTargetWrapper.class.isInstance(parent)) {
            parent = parent.getParent();
        }
        return Optional.ofNullable(parent).map(c -> (ScrollableTargetWrapper) c);
    }
}
