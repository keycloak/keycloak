package org.keycloak.test;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.representations.idm.RequiredCredentialRepresentation;
import org.keycloak.services.managers.InstallationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.RequiredCredentialModel;
import org.keycloak.services.models.RoleModel;
import org.keycloak.services.models.UserCredentialModel;
import org.keycloak.services.models.UserModel;
import org.keycloak.services.models.relationships.RealmAdminRelationship;
import org.keycloak.services.models.relationships.RequiredCredentialRelationship;
import org.keycloak.services.models.relationships.ResourceRelationship;
import org.keycloak.services.models.relationships.ScopeRelationship;
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

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AdapterTest {
    private IdentitySessionFactory factory;
    private IdentitySession IdentitySession;
    private RealmManager adapter;
    private RealmModel realmModel;

    @Before
    public void before() throws Exception {
        factory = createFactory();
        IdentitySession = factory.createIdentitySession();
        adapter = new RealmManager(IdentitySession);
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
        IdentitySession.close();
        factory.close();
    }

    @Test
    public void installTest() throws Exception {
        new InstallationManager().install(adapter);

    }

    @Test
    public void test1CreateRealm() throws Exception {
        realmModel = adapter.createRealm("JUGGLER");
        realmModel.setAccessCodeLifespan(100);
        realmModel.setCookieLoginAllowed(true);
        realmModel.setEnabled(true);
        realmModel.setName("JUGGLER");
        realmModel.setPrivateKeyPem("0234234");
        realmModel.setPublicKeyPem("0234234");
        realmModel.setTokenLifespan(1000);
        realmModel.updateRealm();

        System.out.println(realmModel.getId());
        realmModel = adapter.getRealm(realmModel.getId());
        Assert.assertNotNull(realmModel);
        Assert.assertEquals(realmModel.getAccessCodeLifespan(), 100);
        Assert.assertEquals(realmModel.getTokenLifespan(), 1000);
        Assert.assertEquals(realmModel.isEnabled(), true);
        Assert.assertEquals(realmModel.getName(), "JUGGLER");
        Assert.assertEquals(realmModel.getPrivateKeyPem(), "0234234");
        Assert.assertEquals(realmModel.getPublicKeyPem(), "0234234");
    }

    @Test
    public void test2RequiredCredential() throws Exception {
        test1CreateRealm();
        RequiredCredentialModel creds = new RequiredCredentialModel();
        creds.setSecret(true);
        creds.setType(RequiredCredentialRepresentation.PASSWORD);
        creds.setInput(true);
        realmModel.addRequiredCredential(creds);
        creds = new RequiredCredentialModel();
        creds.setSecret(true);
        creds.setType(RequiredCredentialRepresentation.TOTP);
        creds.setInput(true);
        realmModel.addRequiredCredential(creds);
        List<RequiredCredentialModel> storedCreds = realmModel.getRequiredCredentials();
        Assert.assertEquals(2, storedCreds.size());
        boolean totp = false;
        boolean password = false;
        for (RequiredCredentialModel cred : storedCreds) {
            if (cred.getType().equals(RequiredCredentialRepresentation.PASSWORD)) password = true;
            else if (cred.getType().equals(RequiredCredentialRepresentation.TOTP)) totp = true;
        }
        Assert.assertTrue(totp);
        Assert.assertTrue(password);
    }

    @Test
    public void testCredentialValidation() throws Exception {
        test1CreateRealm();
        UserModel user = realmModel.addUser("bburke");
        UserCredentialModel cred = new UserCredentialModel();
        cred.setType(RequiredCredentialRepresentation.PASSWORD);
        cred.setValue("geheim");
        realmModel.updateCredential(user, cred);
        Assert.assertTrue(realmModel.validatePassword(user, "geheim"));
    }

    @Test
    public void testRoles() throws Exception {
        test1CreateRealm();
        realmModel.addRole("admin");
        realmModel.addRole("user");
        List<RoleModel> roles = realmModel.getRoles();
        Assert.assertEquals(5, roles.size());
        UserModel user = realmModel.addUser("bburke");
        RoleModel role = realmModel.getRole("user");
        realmModel.grantRole(user, role);
        Assert.assertTrue(realmModel.hasRole(user, role));
    }


}
