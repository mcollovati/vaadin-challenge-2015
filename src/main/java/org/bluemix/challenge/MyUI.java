package org.bluemix.challenge;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.annotations.Widgetset;
import com.vaadin.cdi.CDIUI;
import com.vaadin.cdi.CDIViewProvider;
import com.vaadin.cdi.server.VaadinCDIServletService;
import com.vaadin.navigator.Navigator;
import com.vaadin.server.DeploymentConfiguration;
import com.vaadin.server.RequestHandler;
import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.communication.FileUploadHandler;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;

import org.bluemix.challenge.ui.AsyncFileUploadHandler;
import org.vaadin.viewportservlet.ViewPortCDIServlet;

import java.util.List;

import javax.inject.Inject;
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
     * @param e               the exception thrown by Navigator
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
    public static class MyUIServlet extends ViewPortCDIServlet {

        @Override
        protected VaadinServletService createServletService(
                DeploymentConfiguration deploymentConfiguration)
                throws ServiceException {
            VaadinCDIServletService service = new VaadinCDIServletService(this, deploymentConfiguration) {
                @Override
                protected List<RequestHandler> createRequestHandlers() throws ServiceException {
                    List<RequestHandler> handlers = super.createRequestHandlers();
                    handlers.replaceAll(r -> {
                        if (r instanceof FileUploadHandler) {
                            return new AsyncFileUploadHandler();
                        }
                        return r;
                    });
                    return handlers;
                }
            };
            service.init();
            return service;
        }
    }
}
