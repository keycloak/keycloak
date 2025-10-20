package org.keycloak.protocol.ssf.receiver;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

public class DefaultSsfReceiverFactory implements SsfReceiverFactory {

    protected static final Logger log = Logger.getLogger(DefaultSsfReceiverFactory.class);

    @Override
    public String getId() {
        return "default";
    }

    @Override
    public String getHelpText() {
        return "Default Shared Signals Event Receiver";
    }

    @Override
    public SsfReceiver create(KeycloakSession session) {
        return new DefaultSsfReceiver(session);
    }

    @Override
    public SsfReceiver create(KeycloakSession session, ComponentModel model) {
        return new DefaultSsfReceiver(session, model);
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model) throws ComponentValidationException {
        // NOOP
    }

    @Override
    public void onCreate(KeycloakSession session, RealmModel realm, ComponentModel model) {
        log.infof("Created default shared signals receiver for realm '%s'", realm.getId());
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of();
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }
}
