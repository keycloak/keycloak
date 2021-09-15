package org.keycloak.services.scheduled;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Objects;

import com.google.common.collect.Streams;
import org.jboss.logging.Logger;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.broker.saml.SAMLIdentityProvider;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.timer.ScheduledTask;
import org.keycloak.timer.TimerProvider;

public class AutoUpdateIdentityProviders implements ScheduledTask {

    protected static final Logger logger = Logger.getLogger(AutoUpdateIdentityProviders.class);

    protected final String alias;
    protected final String realmId;

    public AutoUpdateIdentityProviders(String alias, String realmId) {
        this.alias = alias;
        this.realmId = realmId;
    }

    @Override
    public void run(KeycloakSession session) {
        logger.info(" Updating identity provider with alias= " + alias + " in realm= " + realmId);
        RealmModel realm = session.realms().getRealm(realmId);
        if ( realm == null) {
            TimerProvider timer = session.getProvider(TimerProvider.class);
            timer.cancelTask(realmId + "_AutoUpdateIdP_" + alias);
        }
        IdentityProviderModel idp = realm.getIdentityProviderByAlias(alias);
        if (idp == null) {
            TimerProvider timer = session.getProvider(TimerProvider.class);
            timer.cancelTask(realmId + "_AutoUpdateIdP_" + alias);
            throw new javax.ws.rs.NotFoundException();
        }
        try {
            InputStream inputStream = session.getProvider(HttpClientProvider.class).get(idp.getConfig().get(IdentityProviderModel.METADATA_URL));
            idp = getProviderFactorytById(session, idp.getProviderId()).parseConfig(session, inputStream, idp);
            idp.getConfig().put(IdentityProviderModel.LAST_REFRESH_TIME, String.valueOf(Instant.now().toEpochMilli()));
            realm.updateIdentityProvider(idp);
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private IdentityProviderFactory getProviderFactorytById(KeycloakSession session, String providerId) {
        return Streams.concat(session.getKeycloakSessionFactory().getProviderFactoriesStream(IdentityProvider.class),
                session.getKeycloakSessionFactory().getProviderFactoriesStream(SocialIdentityProvider.class))
                .filter(providerFactory -> Objects.equals(providerId, providerFactory.getId()))
                .map(IdentityProviderFactory.class::cast)
                .findFirst()
                .orElse(null);
    }


}
