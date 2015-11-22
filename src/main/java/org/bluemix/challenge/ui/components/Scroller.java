package org.bluemix.challenge.ui.components;

import com.vaadin.annotations.JavaScript;
import com.vaadin.server.AbstractClientConnector;
import com.vaadin.server.AbstractJavaScriptExtension;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * Created by marco on 22/11/15.
 */
@JavaScript({"scroller_connector.js"})
public class Scroller extends AbstractJavaScriptExtension {

    private Scroller(UI target) {
        extend(Objects.requireNonNull(target));
    }

    private static Optional<Scroller> findExtension(UI ui) {
        List<Scroller> exts = Objects.requireNonNull(ui).getExtensions().stream()
                .filter( ex -> ex instanceof Scroller)
                .map( ex -> (Scroller)ex)
                .collect(toList());
        if (exts.size() > 1) {
            throw new IllegalStateException("Only one Scroller per UI is allowed");
        }
        return exts.stream().findFirst();
    }
    public static Scroller applyTo(UI target) {
        return findExtension(target)
                .orElseGet(() -> new Scroller(target));
    }

    public static Scroller current() {
        return Optional.ofNullable(UI.getCurrent())
                .flatMap(Scroller::findExtension)
                .orElse(null);
    }
    public void ensureVisible(Component component) {
        ensureVisible(component, false);
    }
    public void ensureVisible(Component component, boolean center) {
        Optional.ofNullable(component).ifPresent( c -> this.doEnsureVisible(c, center));
    }

    private void doEnsureVisible(Component component, boolean center) {
        callFunction("ensureVisible", component.getConnectorId(), center);
    }

}
