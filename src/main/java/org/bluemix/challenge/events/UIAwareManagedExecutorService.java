package org.bluemix.challenge.events;

import com.vaadin.ui.UI;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.enterprise.concurrent.ManagedExecutorService;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by marco on 08/11/15.
 */
@Slf4j
public class UIAwareManagedExecutorService implements ManagedExecutorService{


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
        return delegate.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegate.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(tasks, timeout, unit);
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
        return delegate.submit(task);
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


    static Runnable uiAwareRunnable(Runnable r) {
        UI currentUI = UI.getCurrent();
        return () -> {
            Optional<UI> ui = Optional.ofNullable(UI.getCurrent());
            try {
                if (!ui.isPresent()) {
                    UI.setCurrent(currentUI);
                }
                r.run();
            } finally {
                if (!ui.isPresent()) {
                    UI.setCurrent(null);
                }
            }
        };
    }
}
