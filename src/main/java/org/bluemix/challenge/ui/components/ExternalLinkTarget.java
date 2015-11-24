package org.bluemix.challenge.ui.components;

import com.vaadin.annotations.JavaScript;
import com.vaadin.data.Property;
import com.vaadin.server.AbstractJavaScriptExtension;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Label;

import java.util.Optional;

/**
 * @author Marco Collovati
 */
@JavaScript({"labelLinkTarget_connector.js"})
public class ExternalLinkTarget extends AbstractJavaScriptExtension implements Property.ValueChangeListener {

    private ExternalLinkTarget(Label label) {
        super(label);
        label.addValueChangeListener(this);
    }

    @Override
    public Label getParent() {
        return (Label)super.getParent();
    }

    @Override
    public void remove() {
        Optional.ofNullable(getParent())
                .ifPresent( l -> l.removeValueChangeListener(this));
        super.remove();
    }

    public static <T extends Label> T extend(T label) {
        ExternalLinkTarget extension = new ExternalLinkTarget(label);
        return label;
    }

    @Override
    public void valueChange(Property.ValueChangeEvent event) {
        if (getParent().getContentMode() == ContentMode.HTML) {
            callFunction("scanLinks");
        }
    }
}
