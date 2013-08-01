package org.keycloak.services.resources;

import org.keycloak.SkeletonKeyContextResolver;
import org.keycloak.services.filters.KeycloakSessionRequestFilter;
import org.keycloak.services.filters.KeycloakSessionResponseFilter;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.models.KeycloakSessionFactory;
import org.keycloak.services.models.picketlink.PicketlinkKeycloakSession;
import org.keycloak.services.models.picketlink.PicketlinkKeycloakSessionFactory;
import org.keycloak.services.models.picketlink.mappings.RealmEntity;
import org.keycloak.services.models.picketlink.mappings.ResourceEntity;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.config.IdentityConfigurationBuilder;
import org.picketlink.idm.internal.DefaultPartitionManager;
import org.picketlink.idm.jpa.internal.JPAContextInitializer;
import org.picketlink.idm.jpa.model.sample.simple.AccountTypeEntity;
import org.picketlink.idm.jpa.model.sample.simple.AttributeTypeEntity;
import org.picketlink.idm.jpa.model.sample.simple.AttributedTypeEntity;
import org.picketlink.idm.jpa.model.sample.simple.DigestCredentialTypeEntity;
import org.picketlink.idm.jpa.model.sample.simple.GroupTypeEntity;
import org.picketlink.idm.jpa.model.sample.simple.IdentityTypeEntity;
import org.picketlink.idm.jpa.model.sample.simple.OTPCredentialTypeEntity;
import org.picketlink.idm.jpa.model.sample.simple.PartitionTypeEntity;
import org.picketlink.idm.jpa.model.sample.simple.PasswordCredentialTypeEntity;
import org.picketlink.idm.jpa.model.sample.simple.RelationshipIdentityTypeEntity;
import org.picketlink.idm.jpa.model.sample.simple.RelationshipTypeEntity;
import org.picketlink.idm.jpa.model.sample.simple.RoleTypeEntity;
import org.picketlink.idm.jpa.model.sample.simple.X509CredentialTypeEntity;

import javax.annotation.PreDestroy;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.ws.rs.core.Application;
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

    public KeycloakApplication() {
        KeycloakSessionFactory f = createSessionFactory();
        this.factory = f;
        KeycloakSessionRequestFilter filter = new KeycloakSessionRequestFilter(factory);
        singletons.add(new RealmsResource(new TokenManager()));
        singletons.add(filter);
        classes.add(KeycloakSessionResponseFilter.class);
        classes.add(SkeletonKeyContextResolver.class);
        classes.add(RegistrationService.class);
    }

    protected KeycloakSessionFactory createSessionFactory() {
        return buildSessionFactory();
    }

    public static KeycloakSessionFactory buildSessionFactory() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("keycloak-identity-store");
        return new PicketlinkKeycloakSessionFactory(emf, buildPartitionManager());
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
                        ResourceEntity.class
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
