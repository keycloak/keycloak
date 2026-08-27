package org.keycloak.quarkus.runtime.integration.jaxrs;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import org.jboss.resteasy.reactive.server.core.CurrentRequestManager;
import org.jboss.resteasy.reactive.server.vertx.VertxResteasyReactiveRequestContext;

/*
 * This is only a work-around until the proper fix is back-ported by quarkus.
 * 
 * The strategy here is to prevent the ConnectionCloseHandler from being added
 * to the context close handlers. 
 */
@Provider
@PreMatching
@Priority(1)
public class ConnectionCloseInhibittingFilter implements ContainerRequestFilter {

    private static final Field CLOSE_HANDLERS_FIELD;

    static {
        try {
            CLOSE_HANDLERS_FIELD = VertxResteasyReactiveRequestContext.class.getDeclaredField("closeHandlers");
            CLOSE_HANDLERS_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Failed to access fields", e);
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        var context = (VertxResteasyReactiveRequestContext) CurrentRequestManager.get();
        try {
            @SuppressWarnings("unchecked")
            List<Runnable> closeHandlers = (List<Runnable>) CLOSE_HANDLERS_FIELD.get(context);
            if (closeHandlers == null) {
                final List<Runnable> finalCloseHandlers = new ArrayList<Runnable>() {
                    @Override
                    public boolean add(Runnable e) {
                        if (e.getClass().getName().equals("org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext$ConnectionCloseHandler")) {
                            return false;
                        }
                        return super.add(e);
                    }
                };
                CLOSE_HANDLERS_FIELD.set(context, finalCloseHandlers);
                HttpServerResponse response = context.getContext().response();
                response.closeHandler(new Handler<>() {
                    @Override
                    public void handle(Void v) {
                        for (Runnable handler : finalCloseHandlers) {
                            handler.run();
                        }
                    }
                });
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to get manipulate the close handlers", e);
        }
    }

}
