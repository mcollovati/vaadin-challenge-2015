package org.bluemix.challenge;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.annotations.Widgetset;
import com.vaadin.cdi.CDIUI;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import org.slf4j.bridge.SLF4JBridgeHandler;
import org.vaadin.cdiviewmenu.ViewMenuUI;
import org.vaadin.viewportservlet.ViewPortCDIServlet;

/**
 *
 */
@Theme("mytheme")
@Widgetset("org.bluemix.challenge.MyAppWidgetset")
@CDIUI("")
@Title("Vaadin Challenge by IBM - Services in Bluemix")
public class MyUI extends ViewMenuUI {

    @WebServlet(urlPatterns = "/*", name = "MyUIServlet", asyncSupported = true)
    @VaadinServletConfiguration(ui = MyUI.class, productionMode = false)
    public static class MyUIServlet extends ViewPortCDIServlet {

        static {
            if (SLF4JBridgeHandler.isInstalled()) {
                SLF4JBridgeHandler.uninstall();
            }
            SLF4JBridgeHandler.removeHandlersForRootLogger();
            SLF4JBridgeHandler.install();
        }
    }
}
