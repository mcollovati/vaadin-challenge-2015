package org.bluemix.challenge;

import com.google.common.base.Throwables;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.annotations.Widgetset;
import com.vaadin.cdi.CDIUI;
import com.vaadin.cdi.CDIViewProvider;
import com.vaadin.cdi.server.VaadinCDIServletService;
import com.vaadin.navigator.Navigator;
import com.vaadin.server.*;
import com.vaadin.ui.Component;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bluemix.challenge.io.ImageResource;
import org.bluemix.challenge.io.ImageStorage;
import org.bluemix.challenge.ui.ErrorView;
import org.bluemix.challenge.ui.InsightsView;
import org.bluemix.challenge.ui.StartView;
import org.bluemix.challenge.ui.components.Breadcrumb;
import org.bluemix.challenge.ui.components.Scroller;
import org.vaadin.viewportservlet.ViewPortCDIServlet;
import org.vaadin.viritin.layouts.MVerticalLayout;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Theme("mytheme")
@Widgetset("org.bluemix.challenge.MyAppWidgetset")
@CDIUI("")
@Title("Vaadin Challenge by IBM - Services in Bluemix")
@Push
@Slf4j
public class MyUI extends UI {

    @Inject
    protected CDIViewProvider viewProvider;

    @Inject
    protected ImageStorage imageStorage;

    @Inject
    protected Breadcrumb breadcrumb;

    private Scroller scroller;

    @Override
    protected void init(VaadinRequest request) {

        request.getService().addSessionDestroyListener(ev -> imageStorage.destroy());

        Panel contentLayout = new Panel();
        contentLayout.setStyleName("content-layout");
        contentLayout.addStyleName(ValoTheme.LAYOUT_WELL);
        contentLayout.setSizeFull();

        //Breadcrumb breadcrumb = new Breadcrumb();

        MVerticalLayout mainLayout = new MVerticalLayout(breadcrumb)
                .expand(contentLayout).withStyleName("main-layout");
        mainLayout.setSizeFull();
        Navigator navigator = new Navigator(this, contentLayout) {

            @Override
            public void navigateTo(String navigationState) {
                try {
                    super.navigateTo(navigationState);
                } catch (Exception e) {
                    handleNavigationError(navigationState, e);
                }
            }

        };
        navigator.addViewChangeListener(breadcrumb);
        navigator.addProvider(viewProvider);
        navigator.setErrorView(new ErrorView());
        setContent(mainLayout);

        getSession().setErrorHandler(event -> {
            log.error("Error intercepted", event.getThrowable());
            Throwable t = DefaultErrorHandler.findRelevantThrowable(event.getThrowable());
            Notification.show("Ooops! Something went wrong", t.getMessage(), Notification.Type.ERROR_MESSAGE);
        });
        setResponsive(true);
        if (request.getParameter("test") != null) {
            doTest();
        }
        scroller = Scroller.applyTo(this);
        navigator.navigateTo(StartView.VIEW_NAME);
        //navigator.navigateTo(InsightsView.VIEW_NAME);

    }

    @Override
    public void scrollIntoView(Component component) throws IllegalArgumentException {
        if (getPage().getWebBrowser().isTouchDevice()) {
            scroller.ensureVisible(component);
        }
        // Scroll only for mobile devices
        // else {
            //super.scrollIntoView(component);
        //}
    }

    private void doTest() {

        try {
            for (Path p : Files.list(Paths.get("/tmp", "gallery")).toArray(Path[]::new)) {
                ImageResource res = imageStorage.createResource(p.getFileName().toString()).get();

                IOUtils.copy(new FileInputStream(p.toAbsolutePath().toFile()), res.getOutputStream());
            }
        } catch (IOException e) {
            log.error("Cannot prepare demo gallery", e);
        }

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
    }
}
