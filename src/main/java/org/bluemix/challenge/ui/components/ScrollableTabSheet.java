package org.bluemix.challenge.ui.components;

import com.vaadin.server.Resource;
import com.vaadin.ui.Component;
import com.vaadin.ui.TabSheet;

/**
 * Created by marco on 22/11/15.
 */
public class ScrollableTabSheet extends TabSheet {

    private Component findTabComponent(Component component) {
        return ScrollableTargetWrapper.wrapperFor(component)
                .map(s -> (Component)s)
                .orElse(component);
    }


    @Override
    public Tab getTab(Component c) {
        return super.getTab(findTabComponent(c));
    }

    @Override
    public void setSelectedTab(Component c) {
        super.setSelectedTab(findTabComponent(c));
    }

}
