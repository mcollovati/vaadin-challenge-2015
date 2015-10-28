package org.bluemix.challenge;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.annotations.Widgetset;
import com.vaadin.cdi.CDIUI;
import com.vaadin.cdi.CDIViewProvider;
import com.vaadin.cdi.server.VaadinCDIServlet;
import com.vaadin.navigator.Navigator;
import com.vaadin.server.BootstrapFragmentResponse;
import com.vaadin.server.BootstrapListener;
import com.vaadin.server.BootstrapPageResponse;
import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.SessionInitListener;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.vaadin.viewportservlet.ViewPortCDIServlet;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

/**
 *
 */
@Theme("mytheme")
@Widgetset("org.bluemix.challenge.MyAppWidgetset")
@CDIUI("")
@Title("Vaadin Challenge by IBM - Services in Bluemix")
@Push
public class MyUI extends UI {
    @Inject
    protected CDIViewProvider viewProvider;

    @Override
    protected void init(VaadinRequest request) {
        CssLayout mainLayout = new CssLayout();
        mainLayout.setSizeFull();
        Navigator navigator = new Navigator(this, mainLayout) {

            @Override
            public void navigateTo(String navigationState) {
                try {
                    super.navigateTo(navigationState);
                } catch (Exception e) {
                    handleNavigationError(navigationState, e);
                }
            }

        };
        navigator.addProvider(viewProvider);
        setContent(mainLayout);
    }

    /**
     * Workaround for issue 1, related to vaadin issues: 13566, 14884
     *
     * @param navigationState the view id that was requested
     * @param e the exception thrown by Navigator
     */
    protected void handleNavigationError(String navigationState, Exception e) {
        Notification.show(
                "The requested view (" + navigationState + ") was not available, "
                        + "entering default screen.", Notification.Type.WARNING_MESSAGE);
        if (navigationState != null && !navigationState.isEmpty()) {
            getNavigator().navigateTo("");
        }
        getSession().getErrorHandler().error(new com.vaadin.server.ErrorEvent(e));
    }


    @WebServlet(urlPatterns = "/*", name = "MyUIServlet", asyncSupported = true)
    @VaadinServletConfiguration(ui = MyUI.class, productionMode = false)
    public static class MyUIServlet extends VaadinCDIServlet {

        @Override
        protected void servletInitialized() throws ServletException {
            super.servletInitialized();
            getService().addSessionInitListener(new SessionInitListener() {

                @Override
                public void sessionInit(SessionInitEvent event) throws ServiceException {
                    event.getSession().addBootstrapListener(
                            new BootstrapListener() {

                                @Override
                                public void modifyBootstrapFragment(
                                        BootstrapFragmentResponse response) {
                                    log("Warning, ViewPortCDIServlet does not support fragments.");
                                }

                                @Override
                                public void modifyBootstrapPage(
                                        BootstrapPageResponse response) {
                                    // <meta name="viewport" content="user-scalable=no,initial-scale=1.0">
                                    Document d = response.
                                            getDocument();
                                    Element el = d.
                                            createElement("meta");
                                    el.attr("name", "viewport");
                                    el.attr("content",
                                            getViewPortConfiguration(
                                                    response));
                                    d.getElementsByTag(
                                            "head").get(
                                            0).appendChild(
                                            el);
                                }

                            });

                }
            });

        }

        protected String getViewPortConfiguration(
                BootstrapPageResponse response) {
            return "user-scalable=no,initial-scale=1.0";
        }

    }
}
