package org.bluemix.challenge.cdi;

import com.google.common.base.Throwables;

import com.vaadin.ui.UI;

import java.util.Optional;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by marco on 10/11/15.
 */
@UIUpdate
@Interceptor
@Slf4j
public class UIUpdateInterceptor {

    @AroundInvoke
    public Object interceptOrder(InvocationContext ctx) throws Exception {
        if (!Void.TYPE.equals(ctx.getMethod().getReturnType())) {
            throw new RuntimeException("Method "  + ctx.getMethod() + " has invalid signature for @UIUpdate. Return type must be void.");
        }
        UIUpdate uiUpdate = ctx.getMethod().getAnnotation(UIUpdate.class);
        UI ui = UI.getCurrent();
        if (uiUpdate != null && uiUpdate.access() && ui != null) {
            log.debug("Invoking UI.access for method " + ctx.getMethod());
            ui.access(() -> {
                try {
                    ctx.proceed();
                } catch (Exception ex) {
                    Throwables.propagate(ex);
                }
            });
            return null;
        } else {
            log.debug("UI.access not inovked for {} with annotation {}. UI is {}",
                    ctx.getMethod(), uiUpdate,
                    Optional.ofNullable(ui).map( arg -> String.valueOf(arg.getUIId())).orElse("null"));
        }
        return ctx.proceed();
    }
}
