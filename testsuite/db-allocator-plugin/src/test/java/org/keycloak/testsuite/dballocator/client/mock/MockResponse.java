package org.keycloak.testsuite.dballocator.client.mock;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MockResponse extends Response {

    private final int status;
    private final String entity;

    public MockResponse(int status, String entity) {
        this.status = status;
        this.entity = entity;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public StatusType getStatusInfo() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getEntity() {
        return entity;
    }

    @Override
    public <T> T readEntity(Class<T> aClass) {
        if (aClass.isAssignableFrom(String.class)) {
            return (T) entity;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T readEntity(GenericType<T> genericType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T readEntity(Class<T> aClass, Annotation[] annotations) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T readEntity(GenericType<T> genericType, Annotation[] annotations) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasEntity() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean bufferEntity() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
    }

    @Override
    public MediaType getMediaType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Locale getLanguage() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getLength() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getAllowedMethods() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntityTag getEntityTag() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Date getDate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Date getLastModified() {
        throw new UnsupportedOperationException();
    }

    @Override
    public URI getLocation() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Link> getLinks() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasLink(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Link getLink(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Link.Builder getLinkBuilder(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MultivaluedMap<String, Object> getMetadata() {
        throw new UnsupportedOperationException();
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getHeaderString(String s) {
        throw new UnsupportedOperationException();
    }
}
