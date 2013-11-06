package org.keycloak.models.picketlink;

import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ModelProvider;
import org.keycloak.models.picketlink.mappings.ApplicationEntity;
import org.keycloak.models.picketlink.mappings.RealmEntity;
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

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PicketlinkModelProvider implements ModelProvider {
    @Override
    public KeycloakSessionFactory createFactory() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("picketlink-keycloak-identity-store");
        return new PicketlinkKeycloakSessionFactory(emf, buildPartitionManager());
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

}
