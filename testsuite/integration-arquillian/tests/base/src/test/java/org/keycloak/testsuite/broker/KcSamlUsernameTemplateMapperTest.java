package org.keycloak.testsuite.broker;

import java.util.Collections;
import java.util.List;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.util.WaitUtils;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.keycloak.broker.saml.mappers.UsernameTemplateMapper.PROVIDER_ID;

/**
 * @author <a href="mailto:external.martin.idel@bosch.io">Martin Idel</a>
 */
public class KcSamlUsernameTemplateMapperTest extends AbstractUsernameTemplateMapperTest {

    @Override
    protected void createMapperInIdp(IdentityProviderRepresentation idp, IdentityProviderMapperSyncMode syncMode) {
        IdentityProviderMapperRepresentation usernameTemplateMapper = new IdentityProviderMapperRepresentation();
        usernameTemplateMapper.setName("saml-username-template-mapper");
        usernameTemplateMapper.setIdentityProviderMapper(PROVIDER_ID);
        usernameTemplateMapper.setConfig(ImmutableMap.<String, String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
                .put("template", "${ALIAS}-${ATTRIBUTE.user-attribute}")
                .build());

        IdentityProviderResource idpResource = realm.identityProviders().get(idp.getAlias());
        usernameTemplateMapper.setIdentityProviderAlias(bc.getIDPAlias());
        idpResource.addMapper(usernameTemplateMapper).close();
    }

    @Override
    protected String getMapperTemplate() {
        return "kc-saml-idp-%s";
    }

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcSamlBrokerConfiguration();
    }

    @Test
    public void testLoginWithMissingMappedAttributeShouldFail() {

        IdentityProviderRepresentation idp = setupIdentityProvider();

        IdentityProviderMapperRepresentation usernameMapper = new IdentityProviderMapperRepresentation();
        usernameMapper.setName("missing-attribute-mapper");
        usernameMapper.setIdentityProviderMapper(PROVIDER_ID);
        usernameMapper.setConfig(ImmutableMap.<String, String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.IMPORT.toString())
                .put("template", "${ATTRIBUTE.non-existent-attribute}")
                .put("target", "LOCAL")
                .build());

        IdentityProviderResource idpResource = realm.identityProviders().get(idp.getAlias());
        usernameMapper.setIdentityProviderAlias(bc.getIDPAlias());
        idpResource.addMapper(usernameMapper).close();

        // create a user in the provider realm
        createUserInProviderRealm(Collections.emptyMap());

        // login via IDP
        logInAsUserInIDP();

        WaitUtils.waitForPageToLoad();
        Assert.assertTrue("Should be on update profile page", updateAccountInformationPage.isCurrent());
        // try to update the account info with only the first and last name (no username provided here).
        updateAccountInformationPage.updateAccountInformation("John", "Doe");

        // we should still be in the update profile page, with an error asking to provide the username
        Assert.assertTrue("Should still be on update profile page", updateAccountInformationPage.isCurrent());
        Assert.assertTrue("Should show error about missing username", driver.getPageSource().contains("Please specify username"));

        // no user should be present in the realm with an empty or null username
        List<UserRepresentation> users = adminClient.realm(bc.consumerRealmName()).users().list();
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
        usernameMapper.setIdentityProviderMapper(PROVIDER_ID);
        usernameMapper.setConfig(ImmutableMap.<String, String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.IMPORT.toString())
                .put("template", "${ALIAS}-${ATTRIBUTE.custom-attr}")
                .put("target", "LOCAL")
                .build());

        IdentityProviderResource idpResource = realm.identityProviders().get(idp.getAlias());
        usernameMapper.setIdentityProviderAlias(bc.getIDPAlias());
        idpResource.addMapper(usernameMapper).close();

        // create a user in the provider realm
        createUserInProviderRealm(Collections.emptyMap());

        // log in via IDP
        logInAsUserInIDP();

        WaitUtils.waitForPageToLoad();
        Assert.assertTrue("Should be on update profile page", updateAccountInformationPage.isCurrent());

        // try to update with only first and last name (no username) - should fail
        updateAccountInformationPage.updateAccountInformation("John", "Doe");
        Assert.assertTrue("Should still be on update profile page", updateAccountInformationPage.isCurrent());
        Assert.assertTrue("Should show error about missing username", driver.getPageSource().contains("Please specify username"));

        // now provide a username and verify the user is created
        updateAccountInformationPage.updateAccountInformation("valid-username", "user@example.com", "John", "Doe");
        Assert.assertFalse("Should not be on update profile page", updateAccountInformationPage.isCurrent());

        UserRepresentation user = adminClient.realm(bc.consumerRealmName()).users().search("valid-username").get(0);
        Assert.assertNotNull(user);
        Assert.assertEquals("valid-username", user.getUsername());
    }
}
