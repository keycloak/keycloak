package org.keycloak.services.resources;

import org.keycloak.SkeletonKeyContextResolver;
import org.keycloak.services.filters.KeycloakSessionFilter;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.models.KeycloakSessionFactory;
import org.keycloak.services.models.picketlink.PicketlinkKeycloakSessionFactory;
import org.keycloak.services.models.picketlink.relationships.RealmAdminRelationship;
import org.keycloak.services.models.picketlink.relationships.RequiredCredentialRelationship;
import org.keycloak.services.models.picketlink.relationships.ResourceRelationship;
import org.keycloak.services.models.picketlink.relationships.ScopeRelationship;
import org.picketlink.idm.IdentitySessionFactory;
import org.picketlink.idm.config.IdentityConfiguration;
import org.picketlink.idm.config.IdentityConfigurationBuilder;
import org.picketlink.idm.internal.DefaultIdentitySessionFactory;
import org.picketlink.idm.jpa.internal.ResourceLocalJpaIdentitySessionHandler;
import org.picketlink.idm.jpa.schema.CredentialObject;
import org.picketlink.idm.jpa.schema.CredentialObjectAttribute;
import org.picketlink.idm.jpa.schema.IdentityObject;
import org.picketlink.idm.jpa.schema.IdentityObjectAttribute;
import org.picketlink.idm.jpa.schema.PartitionObject;
import org.picketlink.idm.jpa.schema.RelationshipIdentityObject;
import org.picketlink.idm.jpa.schema.RelationshipObject;
import org.picketlink.idm.jpa.schema.RelationshipObjectAttribute;

import javax.annotation.PreDestroy;
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
        this.factory = new PicketlinkKeycloakSessionFactory(createFactory());
        KeycloakSessionFilter filter = new KeycloakSessionFilter(factory);
        singletons.add(new RealmsResource(new TokenManager()));
        singletons.add(filter);
        classes.add(SkeletonKeyContextResolver.class);
        classes.add(RegistrationService.class);
    }

    public KeycloakSessionFactory getFactory() {
        return factory;
    }

    @PreDestroy
    public void destroy() {
        factory.close();
    }

    public IdentitySessionFactory createFactory() {
        ResourceLocalJpaIdentitySessionHandler handler = new ResourceLocalJpaIdentitySessionHandler("keycloak-identity-store");
        IdentityConfigurationBuilder builder = new IdentityConfigurationBuilder();

        builder
                .stores()
                .jpa()
                .identityClass(IdentityObject.class)
                .attributeClass(IdentityObjectAttribute.class)
                .relationshipClass(RelationshipObject.class)
                .relationshipIdentityClass(RelationshipIdentityObject.class)
                .relationshipAttributeClass(RelationshipObjectAttribute.class)
                .credentialClass(CredentialObject.class)
                .credentialAttributeClass(CredentialObjectAttribute.class)
                .partitionClass(PartitionObject.class)
                .supportAllFeatures()
                .supportRelationshipType(RealmAdminRelationship.class, ResourceRelationship.class, RequiredCredentialRelationship.class, ScopeRelationship.class)
                .setIdentitySessionHandler(handler);

        IdentityConfiguration build = builder.build();
        return new DefaultIdentitySessionFactory(build);
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
