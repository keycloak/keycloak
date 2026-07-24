package org.keycloak.protocol.docker.mapper;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;

class TestAuthenticatedClientSessionModel implements AuthenticatedClientSessionModel {

    private final Map<String, String> notes = new HashMap<>();

    @Override
    public String getId() {
        return null;
    }

    @Override
    public int getTimestamp() {
        return 0;
    }

    @Override
    public void setTimestamp(int timestamp) {

    }

    @Override
    public void detachFromUserSession() {

    }

    @Override
    public UserSessionModel getUserSession() {
        return null;
    }

    @Override
    public String getNote(String name) {
        return notes.get(name);
    }

    @Override
    public void setNote(String name, String value) {
        notes.put(name, value);
    }

    @Override
    public void removeNote(String name) {
        notes.remove(name);
    }

    @Override
    public Map<String, String> getNotes() {
        return notes;
    }

    @Override
    public String getRedirectUri() {
        return null;
    }

    @Override
    public void setRedirectUri(String uri) {

    }

    @Override
    public RealmModel getRealm() {
        return null;
    }

    @Override
    public ClientModel getClient() {
        return null;
    }

    @Override
    public String getAction() {
        return null;
    }

    @Override
    public void setAction(String action) {

    }

    @Override
    public String getProtocol() {
        return null;
    }

    @Override
    public void setProtocol(String method) {

    }
}
