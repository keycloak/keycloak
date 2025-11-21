package org.keycloak.testsuite.broker;

import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.UserProfileResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.storage.UserStorageProvider.EditMode;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPStorageProviderFactory;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.federation.ldap.LDAPTestContext;
import org.keycloak.testsuite.pages.IdpConfirmLinkPage;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.keycloak.models.utils.ModelToRepresentation.toRepresentationWithoutConfig;

import static org.junit.Assert.assertEquals;

public final class KcOidcBrokerLdapReadOnlyTest extends AbstractInitializedBaseBrokerTest {

    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

    @Page
    private IdpConfirmLinkPage confirmLinkPage;

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration() {
            @Override
            public IdentityProviderRepresentation setUpIdentityProvider(IdentityProviderSyncMode syncMode) {
                return super.setUpIdentityProvider(IdentityProviderSyncMode.FORCE);
            }
        };
    }

    @Before
    public void onBefore() {
        createLdapStorageProvider();
        addLdapUser(bc.getUserLogin(), bc.getUserEmail());
    }

    @Test
    public void testDoNotUpdateEmail() {
        // email as optional in both realms
        UserProfileResource userProfile = adminClient.realm(bc.consumerRealmName()).users().userProfile();
        UPConfig upConfig = userProfile.getConfiguration();
        upConfig.getAttribute(UserModel.EMAIL).setRequired(null);
        userProfile.update(upConfig);
        userProfile = adminClient.realm(bc.providerRealmName()).users().userProfile();
        upConfig = userProfile.getConfiguration();
        upConfig.getAttribute(UserModel.EMAIL).setRequired(null);
        userProfile.update(upConfig);

        // federate user and link account
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        logInWithBroker(bc);
        updateAccountInformationPage.updateAccountInformation(bc.getUserLogin(), bc.getUserEmail(), "f", "l");
        confirmLinkPage.clickLinkAccount();
        loginPage.login(bc.getUserLogin(), "Password1");
        appPage.assertCurrent();

        // unset email on the provider realm
        UserRepresentation user = adminClient.realm(bc.providerRealmName()).users().search(bc.getUserLogin()).get(0);
        user.setEmail("");
        adminClient.realm(bc.providerRealmName()).users().get(user.getId()).update(user);

        // logout user on the consumer realm and login again
        user = adminClient.realm(bc.consumerRealmName()).users().search(bc.getUserLogin()).get(0);
        adminClient.realm(bc.consumerRealmName()).users().get(user.getId()).logout();
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        logInWithBroker(bc);
        appPage.assertCurrent();

        // email should remain unchanged
        user = adminClient.realm(bc.consumerRealmName()).users().search(bc.getUserLogin()).get(0);
        assertEquals(bc.getUserEmail(), user.getEmail());
    }

    private void createLdapStorageProvider() {
        String providerName = "ldap";
        String providerId = LDAPStorageProviderFactory.PROVIDER_NAME;

        Map<String,String> ldapConfig = ldapRule.getConfig();
        ldapConfig.put(LDAPConstants.SYNC_REGISTRATIONS, "false");
        ldapConfig.put(LDAPConstants.EDIT_MODE, EditMode.READ_ONLY.name());
        ldapConfig.put(UserStorageProviderModel.IMPORT_ENABLED, "true");
        MultivaluedHashMap<String, String> config = toComponentConfig(ldapConfig);

        UserStorageProviderModel model = new UserStorageProviderModel();
        model.setLastSync(0);
        model.setChangedSyncPeriod(-1);
        model.setFullSyncPeriod(-1);
        model.setName(providerName);
        model.setPriority(0);
        model.setProviderId(providerId);
        model.setConfig(config);

        Response resp = adminClient.realm(bc.consumerRealmName()).components().add(toRepresentationWithoutConfig(model));
        getCleanup().addComponentId(ApiUtil.getCreatedId(resp));
    }

    private MultivaluedHashMap<String, String> toComponentConfig(Map<String, String> ldapConfig) {
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        for (Map.Entry<String, String> entry : ldapConfig.entrySet()) {
            config.add(entry.getKey(), entry.getValue());

        }
        return config;
    }

    private void addLdapUser(String username, String email) {
        String realmName = bc.consumerRealmName();

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session, realmName, null);
            RealmModel appRealm = ctx.getRealm();

            LDAPTestUtils.removeAllLDAPUsers(ctx.getLdapProvider(), appRealm);
            LDAPObject user = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, username, "f", "l", email , new MultivaluedHashMap<>());
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), user, "Password1");
        });
    }
}
