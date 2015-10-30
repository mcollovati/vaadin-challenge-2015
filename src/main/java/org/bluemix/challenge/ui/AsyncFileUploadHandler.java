package org.bluemix.challenge.ui;

import com.github.rjeschke.txtmark.Run;
import com.vaadin.server.ClientConnector;
import com.vaadin.server.StreamVariable;
import com.vaadin.server.UploadException;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.communication.FileUploadHandler;

import org.apache.deltaspike.core.util.ProxyUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.FutureTask;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by marco on 29/10/15.
 */
@Slf4j
public class AsyncFileUploadHandler extends FileUploadHandler {


    @Override
    protected void doHandleSimpleMultipartFileUpload(VaadinSession session, VaadinRequest request, VaadinResponse response, StreamVariable streamVariable, String variableName, ClientConnector owner, String boundary) throws IOException {
        if (request instanceof VaadinServletRequest) {
            VaadinServletRequest httpReq = (VaadinServletRequest) request;
            AsyncContext asyncContext = httpReq.startAsync();
            asyncContext.addListener(new AsyncListener() {
                @Override
                public void onComplete(AsyncEvent event) throws IOException {
                    log.debug("On Complete req");
                    internalSendUploadResponse(request, response);
                }

                @Override
                public void onTimeout(AsyncEvent event) throws IOException {
                    log.debug("On timeout");
                }

                @Override
                public void onError(AsyncEvent event) throws IOException {
                    log.debug("On error", event.getThrowable());
                }

                @Override
                public void onStartAsync(AsyncEvent event) throws IOException {
                    log.debug("OnStart async");
                }
            });
            ServletInputStream inputStream = httpReq.getInputStream();


            PipedOutputStream pos = new PipedOutputStream();
            PipedInputStream pis = new PipedInputStream(pos);


            VaadinRequest vaadinRequest = (VaadinRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{VaadinRequest.class},
                    (proxy, method, args) -> {
                        if ("getInputStream".equals(method.getName())) {
                            return pis;
                        }
                        return method.invoke(request, args);
                    }
            );


            inputStream.setReadListener(

                    new ReadListener() {
                        private static final int MAX_UPLOAD_BUFFER_SIZE = 4 * 1024;

                        byte[] buffer = new byte[MAX_UPLOAD_BUFFER_SIZE];

                        CompletableFuture future;


                        @Override
                        public void onDataAvailable() throws IOException {
                            log.debug("Reading request");
                            int len = -1;
                            while (inputStream.isReady() && (len = inputStream.read(buffer)) >= 0) {
                                pos.write(buffer, 0 , len);
                                if (future == null) {
                                    future = CompletableFuture.runAsync( () -> {
                                        try {
                                            AsyncFileUploadHandler.super.doHandleSimpleMultipartFileUpload(session, vaadinRequest, response, streamVariable, variableName, owner, boundary);
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }).whenComplete( (o,t) -> asyncContext.complete() );
                                }
                            }

                        }

                        @Override
                        public void onAllDataRead() throws IOException {
                            log.debug("All data read");
                            asyncContext.complete();
                        }

                        @Override
                        public void onError(Throwable t) {
                            log.error("Error reading async", t);
                            asyncContext.complete();
                        }
                    }
            );
            //super.doHandleSimpleMultipartFileUpload(session, vaadinRequest, response, streamVariable, variableName, owner, boundary);
        } else {
            super.doHandleSimpleMultipartFileUpload(session, request, response, streamVariable, variableName, owner, boundary);
            internalSendUploadResponse(request, response);
        }
    }

    @Override
    protected void sendUploadResponse(VaadinRequest request, VaadinResponse response) throws IOException {
        //super.sendUploadResponse(request, response);
    }

    private void internalSendUploadResponse(VaadinRequest request, VaadinResponse response) throws IOException {
        super.sendUploadResponse(request, response);
    }
}


