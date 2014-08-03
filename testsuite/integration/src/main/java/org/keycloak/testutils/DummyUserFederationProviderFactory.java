package org.keycloak.testutils;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderFactory;
import org.keycloak.models.UserFederationProviderModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class DummyUserFederationProviderFactory implements UserFederationProviderFactory {
    @Override
    public UserFederationProvider getInstance(KeycloakSession session, UserFederationProviderModel model) {
        return new DummyUserFederationProvider();
    }

    @Override
    public Set<String> getConfigurationOptions() {
        Set<String> list = new HashSet<String>();
        list.add("important.config");
        return list;
    }

    @Override
    public UserFederationProvider create(KeycloakSession session) {
        return new DummyUserFederationProvider();
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "dummy";
    }
}
