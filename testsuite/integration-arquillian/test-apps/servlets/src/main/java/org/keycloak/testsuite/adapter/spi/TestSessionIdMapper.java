package org.keycloak.testsuite.adapter.spi;

import org.keycloak.adapters.spi.SessionIdMapper;

import java.util.HashSet;
import java.util.Set;

public class TestSessionIdMapper implements SessionIdMapper {

    private static final TestSessionIdMapper SINGLETON = new TestSessionIdMapper();

    private static Set<String> whoCalled = new HashSet<>();

    private TestSessionIdMapper() {
    }

    public boolean isCalledBy(String className) {
        return whoCalled.contains(className);
    }

    public static TestSessionIdMapper getInstance() {
        StackTraceElement[] ste = (new Throwable()).getStackTrace();
        for (int i = 0; i < ste.length; i++) {
            whoCalled.add(ste[i].getClassName());
        }
        return SINGLETON;
    }

    @Override
    public boolean hasSession(String id) {
        return false;
    }

    @Override
    public void clear() {
        whoCalled.clear();
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
