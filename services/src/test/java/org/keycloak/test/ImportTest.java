package org.keycloak.test;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.RequiredCredentialModel;
import org.keycloak.services.models.UserModel;
import org.keycloak.services.models.relationships.RealmAdminRelationship;
import org.keycloak.services.models.relationships.RequiredCredentialRelationship;
import org.keycloak.services.models.relationships.ResourceRelationship;
import org.keycloak.services.models.relationships.ScopeRelationship;
import org.keycloak.services.resources.RegistrationService;
import org.picketlink.idm.IdentitySession;
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
import org.picketlink.idm.model.Realm;

import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ImportTest {
    private IdentitySessionFactory factory;
    private IdentitySession identitySession;
    private RealmManager manager;
    private RealmModel realmModel;

    @Before
    public void before() throws Exception {
        factory = createFactory();
        identitySession = factory.createIdentitySession();
        manager = new RealmManager(identitySession);
    }

    public static IdentitySessionFactory createFactory() {
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


    @After
    public void after() throws Exception {
        identitySession.close();
        factory.close();
    }

    @Test
    public void install() throws Exception {
        RealmModel defaultRealm = manager.createRealm(Realm.DEFAULT_REALM, Realm.DEFAULT_REALM);
        defaultRealm.setName(Realm.DEFAULT_REALM);
        defaultRealm.setEnabled(true);
        defaultRealm.setTokenLifespan(300);
        defaultRealm.setAccessCodeLifespan(60);
        defaultRealm.setSslNotRequired(false);
        defaultRealm.setCookieLoginAllowed(true);
        defaultRealm.setRegistrationAllowed(true);
        manager.generateRealmKeys(defaultRealm);
        defaultRealm.updateRealm();
        defaultRealm.addRequiredCredential(RequiredCredentialModel.PASSWORD);
        defaultRealm.addRole(RegistrationService.REALM_CREATOR_ROLE);

        RealmRepresentation rep = KeycloakTestBase.loadJson("testrealm.json");
        RealmModel realm = manager.createRealm("demo", rep.getRealm());
        manager.importRealm(rep, realm);

        UserModel user = realm.getUser("loginclient");
        Assert.assertNotNull(user);
        Set<String> scopes = realm.getScope(user);
        System.out.println("Scopes size: " + scopes.size());
        Assert.assertTrue(scopes.contains("*"));

    }




}
