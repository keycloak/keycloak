package org.keycloak.testsuite.broker;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.protocol.oidc.mappers.HardcodedClaim;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.social.github.GitHubUserAttributeMapper;

import java.util.HashMap;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.keycloak.models.IdentityProviderMapperSyncMode.FORCE;
import static org.keycloak.models.IdentityProviderMapperSyncMode.IMPORT;
import static org.keycloak.models.IdentityProviderMapperSyncMode.LEGACY;
import static org.keycloak.testsuite.broker.KcOidcBrokerConfiguration.HARDOCDED_CLAIM;
import static org.keycloak.testsuite.broker.KcOidcBrokerConfiguration.HARDOCDED_VALUE;
import static org.keycloak.testsuite.broker.KcOidcBrokerConfiguration.USER_INFO_CLAIM;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;

/**
 * @author <a href="mailto:external.martin.idel@bosch.io">Martin Idel</a>
 */
public class JsonUserAttributeMapperTest extends AbstractIdentityProviderMapperTest {

    public static final String USER_ATTRIBUTE = "user-attribute";

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration();
    }

    @Test
    public void loginWithIdentityProviderMapsJsonAttributeToUserAttributeButDoesNotModify() {
        UserRepresentation user = createMapperThenModifyAttribute(IMPORT, "new-value");

        assertUserAttribute(HARDOCDED_VALUE, user);
    }

    @Test
    public void loginWithIdentityProviderDeletesAttributeInForceMode() {
        UserRepresentation user = createMapperThenDeleteAttribute(FORCE);

        assertAbsentUserAttribute(user);
    }

    @Test
    public void loginWithIdentityProviderDoesNotDeleteAttributeInLegacyMode() {
        UserRepresentation user = createMapperThenDeleteAttribute(LEGACY);

        assertUserAttribute(HARDOCDED_VALUE, user);
    }

    @Test
    public void loginWithIdentityProviderModifiesAttributeInForceMode() {
        UserRepresentation user = createMapperThenModifyAttribute(FORCE, "new-value");

        assertUserAttribute("new-value", user);
    }

    @Test
    public void loginWithIdentityProviderAddsUserAttributeInForceNameWhenMapperIsCreatedLater() {
        UserRepresentation user = loginAndThenCreateMapperThenLoginAgain(FORCE);

        assertUserAttribute(HARDOCDED_VALUE, user);
    }

    @Test
    public void loginWithIdentityProviderDoesNotAddUserAttributeInImportNameWhenMapperIsCreatedLater() {
        UserRepresentation user = loginAndThenCreateMapperThenLoginAgain(IMPORT);

        assertAbsentUserAttribute(user);
    }

    private UserRepresentation loginAndThenCreateMapperThenLoginAgain(IdentityProviderMapperSyncMode syncMode) {
        return loginAsUserTwiceWithMapper(syncMode, true, HARDOCDED_CLAIM, HARDOCDED_VALUE);
    }

    private UserRepresentation createMapperThenDeleteAttribute(IdentityProviderMapperSyncMode syncMode) {
        return loginAsUserTwiceWithMapper(syncMode, false, "deleted", "deleted");
    }

    private UserRepresentation createMapperThenModifyAttribute(IdentityProviderMapperSyncMode syncMode, String updatedValue) {
        return loginAsUserTwiceWithMapper(syncMode, false, HARDOCDED_CLAIM, updatedValue);
    }

    private UserRepresentation loginAsUserTwiceWithMapper(
            IdentityProviderMapperSyncMode syncMode, boolean createAfterFirstLogin, String claim, String updatedValue) {
        final IdentityProviderRepresentation idp = setupIdentityProvider();
        if (!createAfterFirstLogin) {
            createGithubProviderMapper(idp, syncMode);
        }
        createUserInProviderRealm(new HashMap<>());

        logInAsUserInIDPForFirstTime();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        if (!createAfterFirstLogin) {
            assertUserAttribute(HARDOCDED_VALUE, user);
        } else {
            assertAbsentUserAttribute(user);
        }

        if (createAfterFirstLogin) {
            createGithubProviderMapper(idp, syncMode);
        }
        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());

        if (!createAfterFirstLogin) {
            updateClaimSentToIDP(claim, updatedValue);
        }

        logInAsUserInIDP();
        return findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
    }

    private void updateClaimSentToIDP(String claim, String updatedValue) {
        ProtocolMapperRepresentation claimMapper = null;
        final ClientRepresentation brokerClient = adminClient.realm(bc.providerRealmName()).clients().findByClientId(BrokerTestConstants.CLIENT_ID).get(0);
        ProtocolMappersResource protocolMappers = adminClient.realm(bc.providerRealmName()).clients().get(brokerClient.getId()).getProtocolMappers();
        for (ProtocolMapperRepresentation representation : protocolMappers.getMappers()) {
            if (representation.getProtocolMapper().equals(HardcodedClaim.PROVIDER_ID)) {
                claimMapper = representation;
            }
        }
        assertThat(claimMapper, notNullValue());
        claimMapper.getConfig().put(HardcodedClaim.CLAIM_VALUE, "{\"" + claim + "\": \"" + updatedValue + "\"}");
        adminClient.realm(bc.providerRealmName()).clients().get(brokerClient.getId()).getProtocolMappers().update(claimMapper.getId(), claimMapper);
    }

    private void assertUserAttribute(String value, UserRepresentation userRep) {
        assertThat(userRep.getAttributes(), notNullValue());
        assertThat(userRep.getAttributes().get(USER_ATTRIBUTE), containsInAnyOrder(value));
    }

    private void assertAbsentUserAttribute(UserRepresentation userRep) {
        assertThat(userRep.getAttributes(), nullValue());
    }

    private void createGithubProviderMapper(IdentityProviderRepresentation idp, IdentityProviderMapperSyncMode syncMode) {
        IdentityProviderMapperRepresentation githubProvider = new IdentityProviderMapperRepresentation();
        githubProvider.setName("json-attribute-mapper");
        githubProvider.setIdentityProviderMapper(GitHubUserAttributeMapper.PROVIDER_ID);
        githubProvider.setConfig(ImmutableMap.<String, String>builder()
            .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
            .put(AbstractJsonUserAttributeMapper.CONF_JSON_FIELD, USER_INFO_CLAIM + "." + HARDOCDED_CLAIM)
            .put(AbstractJsonUserAttributeMapper.CONF_USER_ATTRIBUTE, USER_ATTRIBUTE)
            .build());

        IdentityProviderResource idpResource = realm.identityProviders().get(idp.getAlias());
        githubProvider.setIdentityProviderAlias(bc.getIDPAlias());
        idpResource.addMapper(githubProvider).close();
    }
}
