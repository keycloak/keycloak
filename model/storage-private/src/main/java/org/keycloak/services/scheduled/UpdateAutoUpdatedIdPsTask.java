package org.keycloak.services.scheduled;


import org.jboss.logging.Logger;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.timer.ScheduledTask;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class UpdateAutoUpdatedIdPsTask implements ScheduledTask {

    protected static final Logger logger = Logger.getLogger(UpdateAutoUpdatedIdPsTask.class);
    protected final String realmId;

    public UpdateAutoUpdatedIdPsTask(String realmId) {
        this.realmId = realmId;
    }

    @Override
    public void run(KeycloakSession session) {
        logger.info(" Updating autoupdated identity providers in realm= " + realmId);
        RealmModel realm = session.realms().getRealm(realmId);
        session.getContext().setRealm(realm);
        session.identityProviders().getAllStream(Map.of(IdentityProviderModel.AUTO_UPDATE, "true"), null, null).forEach(idp -> autoUpdateIdP(idp, session));
        realm.setAutoUpdatedIdPsLastRefreshTime(Instant.now().toEpochMilli());

    }

    private void autoUpdateIdP(IdentityProviderModel idp, KeycloakSession session) {
        try {
            String file = session.getProvider(HttpClientProvider.class).getString(idp.getConfig().get(IdentityProviderModel.METADATA_DESCRIPTOR_URL));
            idp = getProviderFactoryById(idp.getProviderId(), session).parseConfig(session, file, idp);
            session.identityProviders().update(idp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private IdentityProviderFactory<?> getProviderFactoryById(String providerId, KeycloakSession session) {
        return Stream.concat(session.getKeycloakSessionFactory().getProviderFactoriesStream(IdentityProvider.class),
                        session.getKeycloakSessionFactory().getProviderFactoriesStream(SocialIdentityProvider.class))
                .filter(providerFactory -> Objects.equals(providerId, providerFactory.getId()))
                .map(IdentityProviderFactory.class::cast)
                .findFirst()
                .orElse(null);
    }

}
