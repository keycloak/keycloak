package org.keycloak.models.mongo.keycloak.adapters;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.entities.AuthenticationLinkEntity;
import org.keycloak.models.entities.AuthenticationProviderEntity;
import org.keycloak.models.entities.CredentialEntity;
import org.keycloak.models.entities.RequiredCredentialEntity;
import org.keycloak.models.entities.SocialLinkEntity;
import org.keycloak.models.mongo.api.MongoStore;
import org.keycloak.models.mongo.impl.MongoStoreImpl;
import org.keycloak.models.mongo.keycloak.config.MongoClientProvider;
import org.keycloak.models.mongo.keycloak.entities.MongoApplicationEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoOAuthClientEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoRealmEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoRoleEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoUserEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoUserSessionEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoUsernameLoginFailureEntity;

/**
 * KeycloakSessionFactory implementation based on MongoDB
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoKeycloakSessionFactory implements KeycloakSessionFactory {
    protected static final Logger logger = Logger.getLogger(MongoKeycloakSessionFactory.class);

    private static final Class<?>[] MANAGED_ENTITY_TYPES = (Class<?>[])new Class<?>[] {
            MongoRealmEntity.class,
            MongoUserEntity.class,
            MongoRoleEntity.class,
            RequiredCredentialEntity.class,
            AuthenticationProviderEntity.class,
            CredentialEntity.class,
            SocialLinkEntity.class,
            AuthenticationLinkEntity.class,
            MongoApplicationEntity.class,
            MongoOAuthClientEntity.class,
            MongoUsernameLoginFailureEntity.class,
            MongoUserSessionEntity.class
    };

    private final MongoClientProvider mongoClientProvider;
    private final MongoStore mongoStore;

    public MongoKeycloakSessionFactory(MongoClientProvider provider) {
        this.mongoClientProvider = provider;
        this.mongoStore = new MongoStoreImpl(provider.getDB(), provider.clearCollectionsOnStartup(), MANAGED_ENTITY_TYPES);
    }

    @Override
    public KeycloakSession createSession() {
        return new MongoKeycloakSession(mongoStore);
    }

    @Override
    public void close() {
        this.mongoClientProvider.close();
    }
}
