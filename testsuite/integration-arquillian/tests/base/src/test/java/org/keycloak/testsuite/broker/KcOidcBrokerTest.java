package org.keycloak.testsuite.broker;

import static org.keycloak.testsuite.broker.BrokerTestConstants.REALM_PROV_NAME;
import static org.keycloak.testsuite.broker.BrokerTestTools.getAuthRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

import java.util.List;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.mappers.ExternalKeycloakRoleToRoleMapper;
import org.keycloak.crypto.Algorithm;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;

public class KcOidcBrokerTest extends AbstractBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcOidcBrokerConfiguration.INSTANCE;
    }

    @Override
    protected Iterable<IdentityProviderMapperRepresentation> createIdentityProviderMappers() {
        IdentityProviderMapperRepresentation attrMapper1 = new IdentityProviderMapperRepresentation();
        attrMapper1.setName("manager-role-mapper");
        attrMapper1.setIdentityProviderMapper(ExternalKeycloakRoleToRoleMapper.PROVIDER_ID);
        attrMapper1.setConfig(ImmutableMap.<String,String>builder()
                .put("external.role", ROLE_MANAGER)
                .put("role", ROLE_MANAGER)
                .build());

        IdentityProviderMapperRepresentation attrMapper2 = new IdentityProviderMapperRepresentation();
        attrMapper2.setName("user-role-mapper");
        attrMapper2.setIdentityProviderMapper(ExternalKeycloakRoleToRoleMapper.PROVIDER_ID);
        attrMapper2.setConfig(ImmutableMap.<String,String>builder()
                .put("external.role", ROLE_USER)
                .put("role", ROLE_USER)
                .build());

        return Lists.newArrayList(attrMapper1, attrMapper2);
    }

    @Test
    public void loginFetchingUserFromUserEndpoint() {
        RealmResource realm = realmsResouce().realm(bc.providerRealmName());
        ClientsResource clients = realm.clients();
        ClientRepresentation brokerApp = clients.findByClientId("brokerapp").get(0);

        try {
            IdentityProviderResource identityProviderResource = realmsResouce().realm(bc.consumerRealmName()).identityProviders().get(bc.getIDPAlias());
            IdentityProviderRepresentation idp = identityProviderResource.toRepresentation();

            idp.getConfig().put(OIDCIdentityProviderConfig.JWKS_URL, getAuthRoot(suiteContext) + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/certs");
            identityProviderResource.update(idp);

            brokerApp.getAttributes().put(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG, Algorithm.RS256);
            brokerApp.getAttributes().put("validateSignature", Boolean.TRUE.toString());
            clients.get(brokerApp.getId()).update(brokerApp);

            driver.navigate().to(getAccountUrl(bc.consumerRealmName()));

            log.debug("Clicking social " + bc.getIDPAlias());
            accountLoginPage.clickSocial(bc.getIDPAlias());

            waitForPage(driver, "log in to", true);

            Assert.assertTrue("Driver should be on the provider realm page right now",
                    driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));

            log.debug("Logging in");
            accountLoginPage.login(bc.getUserLogin(), bc.getUserPassword());

            waitForPage(driver, "update account information", false);

            updateAccountInformationPage.assertCurrent();
            Assert.assertTrue("We must be on correct realm right now",
                    driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));

            log.debug("Updating info on updateAccount page");
            updateAccountInformationPage.updateAccountInformation(bc.getUserLogin(), bc.getUserEmail(), "Firstname", "Lastname");

            UsersResource consumerUsers = adminClient.realm(bc.consumerRealmName()).users();

            int userCount = consumerUsers.count();
            Assert.assertTrue("There must be at least one user", userCount > 0);

            List<UserRepresentation> users = consumerUsers.search("", 0, userCount);

            boolean isUserFound = false;
            for (UserRepresentation user : users) {
                if (user.getUsername().equals(bc.getUserLogin()) && user.getEmail().equals(bc.getUserEmail())) {
                    isUserFound = true;
                    break;
                }
            }

            Assert.assertTrue("There must be user " + bc.getUserLogin() + " in realm " + bc.consumerRealmName(),
                    isUserFound);
        } finally {
            brokerApp.getAttributes().put(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG, null);
            brokerApp.getAttributes().put("validateSignature", Boolean.FALSE.toString());
            clients.get(brokerApp.getId()).update(brokerApp);
        }
    }
}
