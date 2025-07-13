package org.keycloak.services.util;

import jakarta.ws.rs.container.ContainerRequestContext;
import org.jboss.resteasy.reactive.server.core.CurrentRequestManager;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class Projections {

    public static final String PARAM = "fields";

    private static final String REQUEST_PROPERTY = "projections";

    private static final Pattern COMMA = Pattern.compile(",");

    private static final Set<String> INCLUDE_ALL = Collections.unmodifiableSet(new HashSet<>());

    private final Set<String> fields;

    public Projections(String raw) {
        this.fields = raw == null ? INCLUDE_ALL : Set.of(COMMA.split(raw));
    }

    public Set<String> getFields() {
        return fields;
    }

    @Override
    public String toString() {
        return fields.toString();
    }

    public boolean contains(String attr) {
        if (fields == INCLUDE_ALL) {
            return true;
        }
        return fields.contains(attr);
    }

    public static Projections getCurrent() {
        ResteasyReactiveRequestContext context = CurrentRequestManager.get();
        if (context == null) {
            return null;
        }
        return (Projections) context.getProperty(Projections.REQUEST_PROPERTY);
    }

    public static void setCurrent(ContainerRequestContext requestContext, String projectionsParam) {
        // TODO avoid duplicate parsing here and on resource method
        requestContext.setProperty(Projections.REQUEST_PROPERTY, new Projections(projectionsParam));
    }
}
