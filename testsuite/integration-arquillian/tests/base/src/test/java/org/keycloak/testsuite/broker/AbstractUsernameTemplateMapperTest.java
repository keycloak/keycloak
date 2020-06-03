package org.keycloak.testsuite.broker;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;
import static org.keycloak.testsuite.broker.KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;

import java.util.List;

import org.junit.Test;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * @author <a href="mailto:external.martin.idel@bosch.io">Martin Idel</a>,
 */
public abstract class AbstractUsernameTemplateMapperTest extends AbstractIdentityProviderMapperTest {

    protected abstract String getMapperTemplate();

    protected abstract void createMapperInIdp(IdentityProviderRepresentation idp, IdentityProviderMapperSyncMode syncMode);

    @Test
    public void testUsernameGetsInsertedFromClaim() {
        loginAsUserTwiceWithMapperWillNotUpdateUsername(IdentityProviderMapperSyncMode.IMPORT);
    }

    @Test
    public void testUsernameGetsUpdatedFromClaimInForceMode() {
        loginAsUserTwiceWithMapperUpdatesUsername(IdentityProviderMapperSyncMode.FORCE);
    }

    @Test
    public void testUsernameDoesNotGetUpdatedInLegacyMode() {
        loginAsUserTwiceWithMapperWillNotUpdateUsername(IdentityProviderMapperSyncMode.LEGACY);
    }

    private void loginAsUserTwiceWithMapperUpdatesUsername(IdentityProviderMapperSyncMode syncMode) {
        loginAsUserTwiceWithMapper(syncMode, "customusername", "newname", true);
    }

    private void loginAsUserTwiceWithMapperWillNotUpdateUsername(IdentityProviderMapperSyncMode syncMode) {
        loginAsUserTwiceWithMapper(syncMode, "customusername", "newname", false);
    }

    private void loginAsUserTwiceWithMapper(
            IdentityProviderMapperSyncMode syncMode, String userName, String updatedUserName, boolean updatingUserName) {
        final IdentityProviderRepresentation idp = setupIdentityProvider();
        createMapperInIdp(idp, syncMode);
        // The ATTRIBUTE_TO_MAP_NAME gets mapped to a claim by the setup. It's value will always be an array, therefore the [] around the value
        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add(userName).build())
                .build());

        logInAsUserInIDPForFirstTime();

        String mappedUserName = String.format(getMapperTemplate(), userName);
        findUser(bc.consumerRealmName(), mappedUserName, bc.getUserEmail());

        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());

        updateUser(updatedUserName);

        logInAsUserInIDP();
        String updatedMappedUserName = String.format(getMapperTemplate(), updatedUserName);
        UserRepresentation user = findUser(bc.consumerRealmName(), updatingUserName ? updatedMappedUserName : mappedUserName, bc.getUserEmail());
        if (updatingUserName) {
            assertThat(user.getUsername(), is(updatedMappedUserName));
        } else {
            assertThat(user.getUsername(), is(mappedUserName));
        }
    }

    // We don't want to update the username - that needs to be done by the mapper
    @Override
    protected void logInAsUserInIDPForFirstTime() {
        logInAsUserInIDP();

        waitForPage(driver, "update account information", false);

        Assert.assertTrue(updateAccountInformationPage.isCurrent());
        Assert.assertTrue("We must be on correct realm right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));

        log.debug("Updating info on updateAccount page");
        updateAccountInformationPage.updateAccountInformation(bc.getUserEmail(), "FirstName", "LastName");
    }

    private void updateUser(String updatedUserName) {
        UserRepresentation user = findUser(bc.providerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        ImmutableMap<String, List<String>> matchingAttributes = ImmutableMap.<String, List<String>>builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add(updatedUserName).build())
                .build();
        user.setAttributes(matchingAttributes);
        adminClient.realm(bc.providerRealmName()).users().get(user.getId()).update(user);
    }
}
