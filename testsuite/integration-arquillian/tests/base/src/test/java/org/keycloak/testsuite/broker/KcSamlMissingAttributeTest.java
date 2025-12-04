package org.keycloak.testsuite.broker;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.broker.saml.mappers.UsernameTemplateMapper;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;

import java.util.Collections;
import java.util.List;

import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

public class KcSamlMissingAttributeTest extends AbstractIdentityProviderMapperTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcSamlBrokerConfiguration.INSTANCE;
    }

    @Test
    public void testLoginWithMissingMappedAttributeShouldFail() {
    	
        IdentityProviderRepresentation idp = setupIdentityProvider();

        IdentityProviderMapperRepresentation usernameMapper = new IdentityProviderMapperRepresentation();
        usernameMapper.setName("missing-attribute-mapper");
        usernameMapper.setIdentityProviderMapper(UsernameTemplateMapper.PROVIDER_ID);
        usernameMapper.setConfig(ImmutableMap.<String, String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.IMPORT.toString())
                .put("template", "${ATTRIBUTE.non-existent-attribute}")
                .put("target", "LOCAL")
                .build());

        IdentityProviderResource idpResource = realm.identityProviders().get(idp.getAlias());
        usernameMapper.setIdentityProviderAlias(bc.getIDPAlias());
        idpResource.addMapper(usernameMapper).close();

        createUserInProviderRealm(Collections.emptyMap());

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));

        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);

        Assert.assertTrue("Should be on update profile page or show error",
                updateAccountInformationPage.isCurrent() ||
                errorPage.isCurrent());

        UsersResource consumerUsers = adminClient.realm(bc.consumerRealmName()).users();
        List<UserRepresentation> users = consumerUsers.list();

        for (UserRepresentation user : users) {
            Assert.assertNotNull("Username should not be null", user.getUsername());
            Assert.assertFalse("Username should not be empty", user.getUsername().trim().isEmpty());
        }
    }

    @Test
    public void testLoginWithPartiallyMissingAttributeInTemplate() {
    	
        IdentityProviderRepresentation idp = setupIdentityProvider();

        IdentityProviderMapperRepresentation usernameMapper = new IdentityProviderMapperRepresentation();
        usernameMapper.setName("partial-attribute-mapper");
        usernameMapper.setIdentityProviderMapper(UsernameTemplateMapper.PROVIDER_ID);
        usernameMapper.setConfig(ImmutableMap.<String, String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.IMPORT.toString())
                .put("template", "${ALIAS}-${ATTRIBUTE.custom-attr}")
                .put("target", "LOCAL")
                .build());

        IdentityProviderResource idpResource = realm.identityProviders().get(idp.getAlias());
        usernameMapper.setIdentityProviderAlias(bc.getIDPAlias());
        idpResource.addMapper(usernameMapper).close();

        createUserInProviderRealm(Collections.emptyMap());

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        Assert.assertTrue("Should handle missing attribute gracefully, not create invalid user",
                updateAccountInformationPage.isCurrent() || errorPage.isCurrent());

        UsersResource consumerUsers = adminClient.realm(bc.consumerRealmName()).users();
        List<UserRepresentation> users = consumerUsers.list();

        for (UserRepresentation user : users) {
            Assert.assertNotNull("Username should not be null", user.getUsername());
            Assert.assertFalse("Username should not be empty", user.getUsername().trim().isEmpty());
            Assert.assertFalse("Username should not end with dash indicating missing attribute",
                    user.getUsername().matches(".*-\\s*$"));
        }
    }
}
