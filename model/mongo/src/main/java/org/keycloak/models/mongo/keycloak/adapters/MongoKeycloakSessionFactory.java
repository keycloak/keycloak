package org.keycloak.models.mongo.keycloak.adapters;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.mongo.api.MongoEntity;
import org.keycloak.models.mongo.api.MongoStore;
import org.keycloak.models.mongo.impl.MongoStoreImpl;
import org.keycloak.models.mongo.keycloak.config.MongoClientProvider;
import org.keycloak.models.mongo.keycloak.entities.ApplicationEntity;
import org.keycloak.models.mongo.keycloak.entities.CredentialEntity;
import org.keycloak.models.mongo.keycloak.entities.OAuthClientEntity;
import org.keycloak.models.mongo.keycloak.entities.RealmEntity;
import org.keycloak.models.mongo.keycloak.entities.RequiredCredentialEntity;
import org.keycloak.models.mongo.keycloak.entities.RoleEntity;
import org.keycloak.models.mongo.keycloak.entities.SocialLinkEntity;
import org.keycloak.models.mongo.keycloak.entities.UserEntity;

/**
 * KeycloakSessionFactory implementation based on MongoDB
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoKeycloakSessionFactory implements KeycloakSessionFactory {
    protected static final Logger logger = Logger.getLogger(MongoKeycloakSessionFactory.class);

    private static final Class<? extends MongoEntity>[] MANAGED_ENTITY_TYPES = (Class<? extends MongoEntity>[])new Class<?>[] {
            RealmEntity.class,
            UserEntity.class,
            RoleEntity.class,
            RequiredCredentialEntity.class,
            CredentialEntity.class,
            SocialLinkEntity.class,
            ApplicationEntity.class,
            OAuthClientEntity.class
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
