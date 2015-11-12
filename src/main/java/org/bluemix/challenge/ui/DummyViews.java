package org.bluemix.challenge.ui;

/**
 * Created by marco on 12/11/15.
 */

import com.vaadin.cdi.CDIView;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FontAwesome;

import org.vaadin.cdiviewmenu.ViewMenuItem;

public interface DummyViews {

    @CDIView(value = "dummy1")
    @ViewMenuItem(title = "Dummy View1")
    class View1 implements View {

        @Override
        public void enter(ViewChangeListener.ViewChangeEvent event) {

        }
    }
    @CDIView(value = "dummy2")
    @ViewMenuItem(title = "Dummy View2")
    class View2 implements View {

        @Override
        public void enter(ViewChangeListener.ViewChangeEvent event) {

        }
    }
    @CDIView(value = "dummy3")
    @ViewMenuItem(title = "Dummy View3")
    class View3 implements View {

        @Override
        public void enter(ViewChangeListener.ViewChangeEvent event) {

        }
    }
}
