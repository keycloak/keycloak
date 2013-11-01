package org.keycloak.services.resources;

import org.keycloak.SkeletonKeyContextResolver;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.picketlink.PicketlinkKeycloakSession;
import org.keycloak.models.picketlink.PicketlinkKeycloakSessionFactory;
import org.keycloak.models.picketlink.mappings.ApplicationEntity;
import org.keycloak.models.picketlink.mappings.RealmEntity;
import org.keycloak.services.utils.PropertiesManager;
import org.keycloak.services.managers.SocialRequestManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.config.IdentityConfigurationBuilder;
import org.picketlink.idm.internal.DefaultPartitionManager;
import org.picketlink.idm.jpa.internal.JPAContextInitializer;
import org.picketlink.idm.jpa.model.sample.simple.*;

import javax.annotation.PreDestroy;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakApplication extends Application {

    protected Set<Object> singletons = new HashSet<Object>();
    protected Set<Class<?>> classes = new HashSet<Class<?>>();

    protected KeycloakSessionFactory factory;

    public KeycloakApplication(@Context ServletContext context) {
        KeycloakSessionFactory f = createSessionFactory();
        this.factory = f;
        context.setAttribute(KeycloakSessionFactory.class.getName(), factory);
        //classes.add(KeycloakSessionCleanupFilter.class);

        TokenManager tokenManager = new TokenManager();

        singletons.add(new RealmsResource(tokenManager));
        singletons.add(new SaasService(tokenManager));
        singletons.add(new SocialResource(tokenManager, new SocialRequestManager()));
        classes.add(SkeletonKeyContextResolver.class);
        classes.add(QRCodeResource.class);
    }

    protected KeycloakSessionFactory createSessionFactory() {
        return buildSessionFactory();
    }

    public static KeycloakSessionFactory buildSessionFactory() {
        if (PropertiesManager.isMongoSessionFactory()) {
            return buildMongoDBSessionFactory();
        } else if (PropertiesManager.isPicketlinkSessionFactory()) {
            return buildPicketlinkSessionFactory();
        } else {
            throw new IllegalStateException("Unknown session factory type: " + PropertiesManager.getSessionFactoryType());
        }
    }

    private static KeycloakSessionFactory buildPicketlinkSessionFactory() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("keycloak-identity-store");
        return new PicketlinkKeycloakSessionFactory(emf, buildPartitionManager());
    }

    private static KeycloakSessionFactory buildMongoDBSessionFactory() {
        String host = PropertiesManager.getMongoHost();
        int port = PropertiesManager.getMongoPort();
        String dbName = PropertiesManager.getMongoDbName();
        boolean dropDatabaseOnStartup = PropertiesManager.dropDatabaseOnStartup();

        // Create MongoDBSessionFactory via reflection now
        try {
            Class<? extends KeycloakSessionFactory> mongoDBSessionFactoryClass = (Class<? extends KeycloakSessionFactory>)Class.forName("org.keycloak.models.mongo.keycloak.adapters.MongoDBSessionFactory");
            Constructor<? extends KeycloakSessionFactory> constr = mongoDBSessionFactoryClass.getConstructor(String.class, int.class, String.class, boolean.class);
            return constr.newInstance(host, port, dbName, dropDatabaseOnStartup);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public KeycloakSessionFactory getFactory() {
        return factory;
    }

    @PreDestroy
    public void destroy() {
        factory.close();
    }

    public PartitionManager createPartitionManager() {
        return buildPartitionManager();
    }

    public static PartitionManager buildPartitionManager() {
        IdentityConfigurationBuilder builder = new IdentityConfigurationBuilder();

        builder
                .named("KEYCLOAK_JPA_CONFIG")
                .stores()
                .jpa()
                .mappedEntity(
                        AttributedTypeEntity.class,
                        AccountTypeEntity.class,
                        RoleTypeEntity.class,
                        GroupTypeEntity.class,
                        IdentityTypeEntity.class,
                        RelationshipTypeEntity.class,
                        RelationshipIdentityTypeEntity.class,
                        PartitionTypeEntity.class,
                        PasswordCredentialTypeEntity.class,
                        DigestCredentialTypeEntity.class,
                        X509CredentialTypeEntity.class,
                        OTPCredentialTypeEntity.class,
                        AttributeTypeEntity.class,
                        RealmEntity.class,
                        ApplicationEntity.class
                )
                .supportGlobalRelationship(org.picketlink.idm.model.Relationship.class)
                .addContextInitializer(new JPAContextInitializer(null) {
                    @Override
                    public EntityManager getEntityManager() {
                        return PicketlinkKeycloakSession.currentEntityManager.get();
                    }
                })
                .supportAllFeatures();

        DefaultPartitionManager partitionManager = new DefaultPartitionManager(builder.buildAll());
        return partitionManager;
    }


    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }

}
