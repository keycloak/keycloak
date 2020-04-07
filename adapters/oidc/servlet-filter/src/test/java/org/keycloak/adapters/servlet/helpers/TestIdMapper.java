package org.keycloak.adapters.servlet.helpers;

import org.keycloak.adapters.spi.SessionIdMapper;

import java.util.Set;

public class TestIdMapper implements SessionIdMapper {
    @Override
    public boolean hasSession(String id) {
        return false;
    }

    @Override
    public void clear() {

    }

    @Override
    public Set<String> getUserSessions(String principal) {
        return null;
    }

    @Override
    public String getSessionFromSSO(String sso) {
        return null;
    }

    @Override
    public void map(String sso, String principal, String session) {

    }

    @Override
    public void removeSession(String session) {

    }
}
