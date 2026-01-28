package org.keycloak.testsuite.broker;

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.BadRequestException;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.broker.oauth.OAuth2IdentityProviderFactory;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.mappers.HardcodedClaim;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Final class as it's not intended to be overriden. Feel free to remove "final" if you really know what you are doing.
 */
public final class OAuth2BrokerTest extends AbstractAdvancedBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration() {
            @Override
            public IdentityProviderRepresentation setUpIdentityProvider(IdentityProviderSyncMode syncMode) {
                IdentityProviderRepresentation broker = super.setUpIdentityProvider(syncMode);

                broker.setProviderId(OAuth2IdentityProviderFactory.PROVIDER_ID);

                // set the openid scope to be able to access the userinfo endpoint
                broker.getConfig().put("defaultScope", "openid");

                return broker;
            }

            @Override
            public List<ClientRepresentation> createProviderClients() {
                List<ClientRepresentation> clients = super.createProviderClients();
                ClientRepresentation client = clients.get(0);
                List<ProtocolMapperRepresentation> protocolMappers = new ArrayList<>(client.getProtocolMappers());
                ProtocolMapperModel userNameClaim = HardcodedClaim.create("username-claim", "username-claim", "username-claim", "String", true, true, true);
                protocolMappers.add(ModelToRepresentation.toRepresentation(userNameClaim));
                ProtocolMapperModel firstNameClaim = HardcodedClaim.create("first-name-claim", "first-name-claim", "first-name-claim", "String", true, true, true);
                protocolMappers.add(ModelToRepresentation.toRepresentation(firstNameClaim));
                ProtocolMapperModel lastNameClaim = HardcodedClaim.create("last-name-claim", "last-name-claim", "last-name-claim", "String", true, true, true);
                protocolMappers.add(ModelToRepresentation.toRepresentation(lastNameClaim));
                ProtocolMapperModel nameClaim = HardcodedClaim.create("name-claim", "name-claim", "my user", "String", true, true, true);
                protocolMappers.add(ModelToRepresentation.toRepresentation(nameClaim));
                ProtocolMapperModel emailClaim = HardcodedClaim.create("email-claim", "email-claim", "email-claim@keycloak.org", "String", true, true, true);
                protocolMappers.add(ModelToRepresentation.toRepresentation(emailClaim));
                client.setProtocolMappers(protocolMappers);
                return clients;
            }
        };
    }

    @Test
    public void testLoginDefaultConfiguration() {
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        logInWithBroker(bc);

        updateAccountInformationPage.assertCurrent();
        Assert.assertTrue("We must be on correct realm right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));
        log.debug("Updating info on updateAccount page");
        updateAccountInformationPage.updateAccountInformation(bc.getUserLogin(), bc.getUserEmail(), "Firstname", "Lastname");

        List<UserRepresentation> users = adminClient.realm(bc.consumerRealmName()).users().search(bc.getUserLogin());
        Assert.assertEquals(1, users.size());
        UserRepresentation user = users.get(0);
        Assert.assertEquals(bc.getUserLogin(), user.getUsername());
        Assert.assertEquals(bc.getUserEmail(), user.getEmail());
        Assert.assertEquals("Firstname", user.getFirstName());
        Assert.assertEquals("Lastname", user.getLastName());
    }

    @Test
    public void testFetchUserProfileClaims() {
        IdentityProviderResource brokerResource = realmsResouce().realm(bc.consumerRealmName()).identityProviders().get(bc.getIDPAlias());
        IdentityProviderRepresentation broker = brokerResource.toRepresentation();
        broker.getConfig().put("userNameClaim", "username-claim");
        broker.getConfig().put("emailClaim", "email-claim");
        broker.getConfig().put("fullNameClaim", "name-claim");
        brokerResource.update(broker);

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        logInWithBroker(bc);

        List<UserRepresentation> users = adminClient.realm(bc.consumerRealmName()).users().search("username-claim");
        Assert.assertEquals(1, users.size());
        UserRepresentation user = users.get(0);
        Assert.assertEquals("username-claim", user.getUsername());
        Assert.assertEquals("email-claim@keycloak.org", user.getEmail());
        Assert.assertEquals("my", user.getFirstName());
        Assert.assertEquals("user", user.getLastName());
    }

    @Test
    public void testMandatoryFields() {
        assertMandatorySetting("userInfoUrl", "User Info URL not provided");
    }

    private void assertMandatorySetting(String key, String errorMessage) {
        IdentityProviderResource brokerResource = realmsResouce().realm(bc.consumerRealmName()).identityProviders().get(bc.getIDPAlias());
        IdentityProviderRepresentation broker = brokerResource.toRepresentation();

        try {
            broker.getConfig().remove(key);
            brokerResource.update(broker);
            Assert.fail(key + " must be mandatory");
        } catch (BadRequestException bre) {
            Assert.assertTrue(bre.getResponse().readEntity(String.class).contains(errorMessage));
        }
    }

    @Override
    protected Iterable<IdentityProviderMapperRepresentation> createIdentityProviderMappers(IdentityProviderMapperSyncMode syncMode) {
        return List.of();
    }

    @Override
    protected void createAdditionalMapperWithCustomSyncMode(IdentityProviderMapperSyncMode syncMode) {

    }

    @Test
    @Ignore
    @Override
    public void differentMappersCanHaveDifferentSyncModes() {
    }

    @Test
    @Ignore
    @Override
    public void mapperDoesNotGrantNewRoleFromTokenWithSyncModeImport() {
    }

    @Test
    @Ignore
    @Override
    public void mapperGrantsNewRoleFromTokenWithInheritedSyncModeForce() {
    }

    @Test
    @Ignore
    @Override
    public void mapperDoesNotGrantNewRoleFromTokenWithInheritedSyncModeImport() {
    }
}
