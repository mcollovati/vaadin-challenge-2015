package org.bluemix.challenge.cdi;

import com.google.common.base.Throwables;

import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;
import com.vaadin.util.CurrentInstance;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.enterprise.concurrent.ManagedExecutorService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static java.util.stream.Collectors.toList;

/**
 * Wraps runnables and callbacks in order to set current UI
 * when them managed executor does not propagate context.
 * Problem was found only on WebSphere Application Server 8.5.5.7/wlp-1.0.10
 *
 * Created by marco on 08/11/15.
 */
@Slf4j
public class UIAwareManagedExecutorService implements ManagedExecutorService{


    /**
     * Wraps ManagedExecutionService only if running on WebSphere
     */
    public static ManagedExecutorService makeUIAware(ManagedExecutorService delegate) {
        try {
            Class<?> wasManagedExecutorServiceClazz = delegate.getClass()
                    .getClassLoader().loadClass("com.ibm.ws.concurrent.internal.ManagedExecutorServiceImpl");
            if (wasManagedExecutorServiceClazz.isInstance(delegate)) {
                log.debug("WAS: creating UIAwareManagedExecutorService");
                return new UIAwareManagedExecutorService(delegate);
            }
            log.debug("WAS, but delegate is " + delegate.getClass());
        } catch (Throwable t) {
            log.debug("Not on WAS");
        }
        return delegate;
    }

    private final ManagedExecutorService delegate;
    private UIAwareManagedExecutorService(ManagedExecutorService delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(mapCallables(tasks));
    }

    private <T> Collection<? extends Callable<T>> mapCallables(Collection<? extends Callable<T>> tasks) {
        return tasks.stream().map(UIAwareManagedExecutorService::uiAwareCallable).collect(toList());
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.invokeAll(mapCallables(tasks), timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegate.invokeAny(mapCallables(tasks));
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(mapCallables(tasks), timeout, unit);
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }


    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(uiAwareCallable(task));
    }

    @Override
    public Future<?> submit(Runnable task) {
        return delegate.submit(uiAwareRunnable(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(uiAwareRunnable(task), result);
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(uiAwareRunnable(command));
    }


    static <T> Callable<T> uiAwareCallable(Callable<T> callable) {
        if (callable instanceof VaadinAwareCallable) {
            return callable;
        }
        return new VaadinAwareCallable<>(callable);
    }

    static Runnable uiAwareRunnable(Runnable r) {
        if (r instanceof VaadinAwareRunnable) {
            return r;
        }
        return new VaadinAwareRunnable(r);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static class VaadinAwareCallable<T> extends VaadinContextSupport implements Callable<T> {

        private final Callable<T> delegate;

        @Override
        public T call() throws Exception {
            return doWork(delegate);
        }
    }

    private static class VaadinAwareRunnable extends VaadinContextSupport implements Runnable {
        private final Callable<Void> delegate;

        public VaadinAwareRunnable(Runnable r) {
            this.delegate = () -> { r.run(); return null; };
        }

        @Override
        public void run() {
            doWork(delegate);
        }
    }

    private static class VaadinContextSupport {
        protected final UI callerUI;


        public VaadinContextSupport() {
            this.callerUI = UI.getCurrent();
        }

        /*
        protected Optional<Map<Class<?>, CurrentInstance>> restoreVaadinContext(Optional<UI> ui) {

            if (!ui.isPresent()) {
                CurrentInstance.setCurrent(callerUI);
                UI.setCurrent(callerUI);
                VaadinSession.setCurrent(callerUI.getSession());
                VaadinService.setCurrent(VaadinSession.getCurrent().getService());
            }
        }
        protected void resetVaadinContext(Optional<UI> ui) {
            if (!ui.isPresent()) {

                UI.setCurrent(null);
                VaadinSession.setCurrent(null);
                VaadinService.setCurrent(null);
            }
        }
        */

        protected <T> T doWork(Callable<T> callable) {
            Optional<UI> ui = Optional.ofNullable(UI.getCurrent());
            Optional<Map<Class<?>, CurrentInstance>> oldInstances =
                    ui.map( arg -> Optional.<Map<Class<?>, CurrentInstance>>empty())
                    .orElse(Optional.ofNullable(CurrentInstance.setCurrent(callerUI)));

            //restoreVaadinContext(ui);
            try {
                return callable.call();
            } catch (Throwable t) {
                log.debug("Error running runnable on executor", t);
                Throwables.propagate(t);
                return null;
            } finally {
                oldInstances.ifPresent(CurrentInstance::restoreInstances);
                //resetVaadinContext(ui);
            }
        }

    }

}
