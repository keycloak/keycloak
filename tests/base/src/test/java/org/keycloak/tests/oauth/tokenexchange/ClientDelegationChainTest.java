package org.keycloak.tests.oauth.tokenexchange;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.ws.rs.NotFoundException;

import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.protocol.oidc.mappers.HardcodedClaim;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.tokenexchange.DelegationChain;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.representations.idm.authorization.AggregatePolicyRepresentation;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.executor.ClientDelegationExecutor;
import org.keycloak.services.clientpolicy.executor.ClientDelegationExecutorFactory;
import org.keycloak.services.clientpolicy.executor.DownscopeAssertionGrantEnforcerExecutorFactory;
import org.keycloak.services.clientpolicy.executor.RejectMayActClaimExecutorFactory;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.DefaultOAuthClientConfiguration;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientPolicyBuilder;
import org.keycloak.testframework.realm.ClientProfileBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.OAuthGrantPage;
import org.keycloak.tests.admin.authz.fgap.PermissionTestUtils;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.LogoutResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.keycloak.OAuth2Constants.ACCESS_TOKEN_TYPE;
import static org.keycloak.OAuth2Constants.CLIENT_ID;
import static org.keycloak.representations.IDToken.ACT;
import static org.keycloak.representations.IDToken.MAY_ACT;
import static org.keycloak.representations.JsonWebToken.SUBJECT;
import static org.keycloak.tests.oauth.tokenexchange.DelegationAssertions.assertMayActNotPresent;
import static org.keycloak.tests.oauth.tokenexchange.DelegationAssertions.assertScopeContains;

/**
 * Tests multi-hop token exchange delegation, where a delegated token is exchanged again by the next client in the
 * chain, and the "client-delegation" client policy executor that decides whether a chain may continue at all.
 */
@KeycloakIntegrationTest(config = ClientDelegationChainTest.ServerConfig.class)
public class ClientDelegationChainTest {

    static final String USERNAME = "test-user@localhost";
    static final String OTHER_USERNAME = "otheruser";
    static final String PASSWORD = "password";

    static final String SUBJECT_CLIENT_ID = "test-app";
    static final String SUBJECT_CLIENT_SECRET = "test-secret";
    static final String AGENT_CLIENT_ID = "agent-app";
    static final String AGENT_CLIENT_SECRET = "agent-secret";
    static final String MCP_CLIENT_ID = "mcp-app";
    static final String MCP_CLIENT_SECRET = "mcp-secret";
    static final String DOWNSTREAM_CLIENT_ID = "downstream-app";
    static final String DOWNSTREAM_CLIENT_SECRET = "downstream-secret";

    static final String AGENT_DELEGATION_SCOPE = OIDCLoginProtocolFactory.CLIENT_DELEGATION_SCOPE
            + ClientScopeModel.VALUE_SEPARATOR + AGENT_CLIENT_ID;

    // the role that makes a client reachable, held by the user and put into the scope of whoever may target it
    static final String TARGET_ROLE = "delegation-target";

    static final String HARDCODED_ACT_MAPPER = "hardcoded-act";
    static final String HARDCODED_MAY_ACT_MAPPER = "hardcoded-may-act";

    // the "delegation_chain" event detail lists the actors in the order they took part
    static final String AGENT_MCP_CHAIN = AGENT_CLIENT_ID + "," + MCP_CLIENT_ID;
    static final String AGENT_MCP_DOWNSTREAM_CHAIN = AGENT_MCP_CHAIN + "," + DOWNSTREAM_CLIENT_ID;

    @InjectRealm(config = DelegationChainRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient(config = SubjectAppConfig.class)
    OAuthClient oauth;

    @InjectClient(config = AgentAppConfig.class, ref = "agent")
    ManagedClient agentApp;

    @InjectClient(config = McpAppConfig.class, ref = "mcp")
    ManagedClient mcpApp;

    @InjectClient(config = DownstreamAppConfig.class, ref = "downstream")
    ManagedClient downstreamApp;

    @InjectEvents
    Events events;

    @InjectPage
    OAuthGrantPage grantPage;

    @BeforeEach
    public void beforeEach() {
        addDelegatePermission();
        addDelegationTargets();
    }

    @AfterEach
    public void afterEach() {
        for (String user : List.of(USERNAME, OTHER_USERNAME)) {
            AccountHelper.logout(realm.admin(), user);
            List<Map<String, Object>> consents = AccountHelper.getUserConsents(realm.admin(), user);
            if (consents.stream().anyMatch(m -> SUBJECT_CLIENT_ID.equals(m.get("clientId")))) {
                AccountHelper.revokeConsents(realm.admin(), user, SUBJECT_CLIENT_ID);
            }
        }
    }

    @Test
    public void threeHopChain() {
        addChainPolicy(chainingEnabled());

        AccessTokenResponse loginRes = login(AGENT_DELEGATION_SCOPE);
        assertScopeContains(loginRes.getScope(), AGENT_DELEGATION_SCOPE);

        // hop 1 is authorized by the "may_act" claim the user consented to
        AccessTokenResponse hop1 = assertExchangeSucceeds(AGENT_CLIENT_ID, AGENT_CLIENT_SECRET, loginRes.getAccessToken(), MCP_CLIENT_ID, 1, null);
        AccessToken agentToken = oauth.verifyToken(hop1.getAccessToken());
        assertActChain(agentToken, AGENT_CLIENT_ID);
        Assertions.assertEquals(USERNAME, agentToken.getPreferredUsername());
        // mcp-app is in the audience because its role is in agent-app's scope and the user holds that role
        Assertions.assertTrue(agentToken.hasAudience(MCP_CLIENT_ID));

        // hop 2 has no "may_act" of its own, it continues the chain of the subject token
        AccessTokenResponse hop2 = assertExchangeSucceeds(MCP_CLIENT_ID, MCP_CLIENT_SECRET, hop1.getAccessToken(), DOWNSTREAM_CLIENT_ID, 2, AGENT_MCP_CHAIN);
        AccessToken mcpToken = oauth.verifyToken(hop2.getAccessToken());
        assertActChain(mcpToken, MCP_CLIENT_ID, AGENT_CLIENT_ID);
        Assertions.assertEquals(USERNAME, mcpToken.getPreferredUsername());
        Assertions.assertNull(mcpToken.getSessionId(), "Delegated token session should be transient");

        AccessTokenResponse hop3 = assertExchangeSucceeds(DOWNSTREAM_CLIENT_ID, DOWNSTREAM_CLIENT_SECRET, hop2.getAccessToken(), 3, AGENT_MCP_DOWNSTREAM_CHAIN);
        AccessToken downstreamToken = oauth.verifyToken(hop3.getAccessToken());
        assertActChain(downstreamToken, DOWNSTREAM_CLIENT_ID, MCP_CLIENT_ID, AGENT_CLIENT_ID);
        Assertions.assertEquals(USERNAME, downstreamToken.getPreferredUsername());

        logout(loginRes.getRefreshToken());
    }

    @Test
    public void maxChainDepthEnforced() {
        addChainPolicy(chainingEnabled());
        // the login token arrives with a chain one actor short of the limit, as no actor may take part twice
        addHardcodedMapper(SUBJECT_CLIENT_ID, forgedActChainMapper(DelegationChain.MAX_CHAIN_DEPTH - 1));

        AccessTokenResponse loginRes = login(null);

        // agent-app is the last actor that still fits
        String actorToken = clientCredentialsToken(AGENT_CLIENT_ID, AGENT_CLIENT_SECRET);
        AccessTokenResponse hop = exchange(AGENT_CLIENT_ID, AGENT_CLIENT_SECRET, loginRes.getAccessToken(), actorToken, MCP_CLIENT_ID);
        Assertions.assertTrue(hop.isSuccess(), hop.getError() + " - " + hop.getErrorDescription());
        events.poll();
        Assertions.assertEquals(DelegationChain.MAX_CHAIN_DEPTH, actorCount(oauth.verifyToken(hop.getAccessToken())));

        assertExchangeRejected(MCP_CLIENT_ID, MCP_CLIENT_SECRET, hop.getAccessToken(),
                "Delegation chain depth " + (DelegationChain.MAX_CHAIN_DEPTH + 1)
                        + " exceeds the maximum of " + DelegationChain.MAX_CHAIN_DEPTH);

        logout(loginRes.getRefreshToken());
    }

    @Test
    public void chainingDisabledByDefault() {
        AccessTokenResponse loginRes = login(AGENT_DELEGATION_SCOPE);

        // a single delegation hop works with no policy at all
        AccessTokenResponse hop1 = assertExchangeSucceeds(AGENT_CLIENT_ID, AGENT_CLIENT_SECRET, loginRes.getAccessToken(), 1, null);

        assertExchangeRejected(MCP_CLIENT_ID, MCP_CLIENT_SECRET, hop1.getAccessToken(),
                "Chaining an already delegated token is not allowed");

        logout(loginRes.getRefreshToken());
    }

    @Test
    public void actorRepetitionRejected() {
        addChainPolicy(chainingEnabled());

        AccessTokenResponse loginRes = login(AGENT_DELEGATION_SCOPE);

        AccessTokenResponse hop1 = assertExchangeSucceeds(AGENT_CLIENT_ID, AGENT_CLIENT_SECRET, loginRes.getAccessToken(), MCP_CLIENT_ID, 1, null);
        AccessTokenResponse hop2 = assertExchangeSucceeds(MCP_CLIENT_ID, MCP_CLIENT_SECRET, hop1.getAccessToken(), AGENT_CLIENT_ID, 2, AGENT_MCP_CHAIN);

        // agent-app is already part of the chain, so handing the token back to it is a loop
        assertExchangeRejected(AGENT_CLIENT_ID, AGENT_CLIENT_SECRET, hop2.getAccessToken(),
                "Actor is already part of the delegation chain in the subject_token");

        logout(loginRes.getRefreshToken());
    }

    @Test
    public void delegatePermissionRevokedMidChain() {
        addChainPolicy(chainingEnabled());

        AccessTokenResponse loginRes = login(AGENT_DELEGATION_SCOPE);

        AccessTokenResponse hop1 = assertExchangeSucceeds(AGENT_CLIENT_ID, AGENT_CLIENT_SECRET, loginRes.getAccessToken(), MCP_CLIENT_ID, 1, null);

        // every hop re-checks the "delegate" permission, so revoking it stops the chain where it stands
        removeDelegatePermissions();

        assertExchangeRejected(MCP_CLIENT_ID, MCP_CLIENT_SECRET, hop1.getAccessToken(),
                "Actor is not allowed to delegate as the subject user");

        logout(loginRes.getRefreshToken());
    }

    @Test
    public void delegatePermissionRevokedAfterConsent() {
        AccessTokenResponse loginRes = login(AGENT_DELEGATION_SCOPE);

        // the "may_act" claim records the consent, but the permission behind it is re-checked at exchange time
        removeDelegatePermissions();

        assertExchangeRejected(AGENT_CLIENT_ID, AGENT_CLIENT_SECRET, loginRes.getAccessToken(),
                "Actor is not allowed to delegate as the subject user");

        logout(loginRes.getRefreshToken());
    }

    @Test
    public void humanActorCannotContinueChain() {
        addChainPolicy(chainingEnabled());

        AccessTokenResponse loginRes = login(AGENT_DELEGATION_SCOPE);

        AccessTokenResponse hop1 = assertExchangeSucceeds(AGENT_CLIENT_ID, AGENT_CLIENT_SECRET, loginRes.getAccessToken(), MCP_CLIENT_ID, 1, null);

        String humanActorToken = passwordGrantToken(MCP_CLIENT_ID, MCP_CLIENT_SECRET, OTHER_USERNAME);
        assertExchangeRejected(MCP_CLIENT_ID, MCP_CLIENT_SECRET, hop1.getAccessToken(), humanActorToken,
                "Only a service account actor can delegate without a may_act claim in the subject_token");

        logout(loginRes.getRefreshToken());
    }

    @Test
    public void adminDelegationTokenCannotSeedChain() {
        addChainPolicy(chainingEnabled());

        String otherUserId = AdminApiUtil.findUserByUsername(realm.admin(), OTHER_USERNAME).getId();

        String scope = OIDCLoginProtocolFactory.USER_DELEGATION_SCOPE + ClientScopeModel.VALUE_SEPARATOR + OTHER_USERNAME;
        AccessTokenResponse loginRes = login(scope);
        assertScopeContains(loginRes.getScope(), scope);

        // a human actor gets an "act" claim with no client_id, as the delegation is not bound to any client
        String humanActorToken = passwordGrantToken(MCP_CLIENT_ID, MCP_CLIENT_SECRET, OTHER_USERNAME);
        AccessTokenResponse hop1 = exchange(MCP_CLIENT_ID, MCP_CLIENT_SECRET, loginRes.getAccessToken(), humanActorToken, AGENT_CLIENT_ID);
        Assertions.assertTrue(hop1.isSuccess(), hop1.getError() + " - " + hop1.getErrorDescription());
        events.poll();
        AccessToken adminDelegatedToken = oauth.verifyToken(hop1.getAccessToken());
        Assertions.assertEquals(otherUserId, actClaim(adminDelegatedToken).get(SUBJECT));
        Assertions.assertNull(actClaim(adminDelegatedToken).get(CLIENT_ID), "act.client_id should not be set for admin delegation");

        assertExchangeRejected(AGENT_CLIENT_ID, AGENT_CLIENT_SECRET, hop1.getAccessToken(),
                "Only a token delegated to a client can be chained, this one was delegated to a user");

        logout(loginRes.getRefreshToken());
    }

    @Test
    public void actorTokenOfAnotherClientRejected() {
        addChainPolicy(chainingEnabled());

        AccessTokenResponse loginRes = login(AGENT_DELEGATION_SCOPE);

        AccessTokenResponse hop1 = assertExchangeSucceeds(AGENT_CLIENT_ID, AGENT_CLIENT_SECRET, loginRes.getAccessToken(), MCP_CLIENT_ID, 1, null);

        // mcp-app asks for the exchange but presents the actor token of downstream-app
        String foreignActorToken = clientCredentialsToken(DOWNSTREAM_CLIENT_ID, DOWNSTREAM_CLIENT_SECRET);
        assertExchangeRejected(MCP_CLIENT_ID, MCP_CLIENT_SECRET, hop1.getAccessToken(), foreignActorToken,
                "Requesting client does not match the client the actor_token was issued for");

        logout(loginRes.getRefreshToken());
    }

    @Test
    public void actorTokenOfForeignServiceAccountRejected() {
        addChainPolicy(chainingEnabled());

        // mcp-app has to sit in the audience of downstream-app's own token to be able to exchange it
        allowTarget(downstreamApp.getId(), mcpApp);
        grantTargetRole(serviceAccountId(DOWNSTREAM_CLIENT_ID), mcpApp);

        AccessTokenResponse loginRes = login(AGENT_DELEGATION_SCOPE);

        AccessTokenResponse hop1 = assertExchangeSucceeds(AGENT_CLIENT_ID, AGENT_CLIENT_SECRET, loginRes.getAccessToken(), MCP_CLIENT_ID, 1, null);

        // fetched first, as doing it inline would reset the shared client config back to downstream-app
        String downstreamToken = clientCredentialsToken(DOWNSTREAM_CLIENT_ID, DOWNSTREAM_CLIENT_SECRET);

        // a plain exchange gives mcp-app a token issued for itself but carrying downstream-app's service account
        AccessTokenResponse borrowed = oauth.client(MCP_CLIENT_ID, MCP_CLIENT_SECRET).scope(null)
                .tokenExchangeRequest(downstreamToken).send();
        Assertions.assertTrue(borrowed.isSuccess(), borrowed.getError() + " - " + borrowed.getErrorDescription());
        AccessToken borrowedToken = oauth.verifyToken(borrowed.getAccessToken());
        Assertions.assertEquals(MCP_CLIENT_ID, borrowedToken.getIssuedFor());
        Assertions.assertEquals(serviceAccountId(DOWNSTREAM_CLIENT_ID), borrowedToken.getSubject());
        events.poll();

        // downstream-app is the one holding the delegate permission, so "azp" alone must not let mcp-app borrow it
        assertExchangeRejected(MCP_CLIENT_ID, MCP_CLIENT_SECRET, hop1.getAccessToken(), borrowed.getAccessToken(),
                "Actor token subject is not the service account of the requesting client");

        logout(loginRes.getRefreshToken());
    }

    @Test
    public void delegationWithoutUserConsentRejected() {
        // the first hop always needs a delegation the user consented to, which the "may_act" claim records
        AccessTokenResponse loginRes = login(null);

        assertExchangeRejected(AGENT_CLIENT_ID, AGENT_CLIENT_SECRET, loginRes.getAccessToken(),
                "The subject_token carries no may_act claim, so the user did not consent to this delegation");

        logout(loginRes.getRefreshToken());
    }

    @Test
    public void mayActAndChainExecutorsCombined() {
        realm.updateWithCleanup(r -> r
                .clientProfile(ClientProfileBuilder.create().name("delegation")
                        .executor(RejectMayActClaimExecutorFactory.PROVIDER_ID, null)
                        .executor(ClientDelegationExecutorFactory.PROVIDER_ID, chainingEnabled())
                        .build())
                .clientPolicy(ClientPolicyBuilder.create().name("delegation-policy")
                        .condition(AnyClientConditionFactory.PROVIDER_ID, null).profile("delegation").build()));

        AccessTokenResponse loginRes = login(AGENT_DELEGATION_SCOPE);
        assertScopeContains(loginRes.getScope(), AGENT_DELEGATION_SCOPE);

        AccessTokenResponse hop1 = assertExchangeSucceeds(AGENT_CLIENT_ID, AGENT_CLIENT_SECRET, loginRes.getAccessToken(), MCP_CLIENT_ID, 1, null);
        AccessTokenResponse hop2 = assertExchangeSucceeds(MCP_CLIENT_ID, MCP_CLIENT_SECRET, hop1.getAccessToken(), 2, AGENT_MCP_CHAIN);
        assertActChain(oauth.verifyToken(hop2.getAccessToken()), MCP_CLIENT_ID, AGENT_CLIENT_ID);

        logout(loginRes.getRefreshToken());
    }

    @Test
    public void mayActStrippedFromDelegatedToken() {
        addChainPolicy(chainingEnabled());
        // a hardcoded mapper is how a may_act claim reaches a token without going through the delegation scope
        addHardcodedMapper(MCP_CLIENT_ID, hardcodedClaimMapper(HARDCODED_MAY_ACT_MAPPER, MAY_ACT + "." + SUBJECT));

        AccessTokenResponse loginRes = login(AGENT_DELEGATION_SCOPE);
        AccessTokenResponse hop1 = assertExchangeSucceeds(AGENT_CLIENT_ID, AGENT_CLIENT_SECRET, loginRes.getAccessToken(), MCP_CLIENT_ID, 1, null);

        // the mapper really writes the claim for mcp-app, so the delegated token below had one to drop
        String actorToken = clientCredentialsToken(MCP_CLIENT_ID, MCP_CLIENT_SECRET);
        Assertions.assertNotNull(oauth.verifyToken(actorToken).getOtherClaims().get(MAY_ACT), "may_act claim should be present");

        AccessTokenResponse hop2 = exchange(MCP_CLIENT_ID, MCP_CLIENT_SECRET, hop1.getAccessToken(), actorToken, DOWNSTREAM_CLIENT_ID);
        Assertions.assertTrue(hop2.isSuccess(), hop2.getError() + " - " + hop2.getErrorDescription());
        events.poll();

        // the delegated token records the chain in "act" only, so the next hop cannot be authorized two ways at once
        AccessToken delegatedToken = oauth.verifyToken(hop2.getAccessToken());
        assertMayActNotPresent(delegatedToken);
        assertActChain(delegatedToken, MCP_CLIENT_ID, AGENT_CLIENT_ID);

        logout(loginRes.getRefreshToken());
    }

    @Test
    public void subjectTokenWithBothDelegationClaimsRejected() {
        // a hardcoded "act" turns the consented login token into one carrying both delegation claims
        addHardcodedMapper(SUBJECT_CLIENT_ID, hardcodedClaimMapper(HARDCODED_ACT_MAPPER, ACT + "." + SUBJECT));

        AccessTokenResponse loginRes = login(AGENT_DELEGATION_SCOPE);
        AccessToken loginToken = oauth.verifyToken(loginRes.getAccessToken());
        Assertions.assertNotNull(loginToken.getOtherClaims().get(MAY_ACT), "may_act claim should be present");
        Assertions.assertNotNull(loginToken.getOtherClaims().get(ACT), "act claim should be present");

        assertExchangeRejected(AGENT_CLIENT_ID, AGENT_CLIENT_SECRET, loginRes.getAccessToken(),
                "The subject_token cannot carry both a may_act and an act claim");

        logout(loginRes.getRefreshToken());
    }

    @Test
    public void hopOutsideTokenAudienceRejected() {
        addChainPolicy(chainingEnabled());

        AccessTokenResponse loginRes = login(AGENT_DELEGATION_SCOPE);

        // agent-app only has the role of mcp-app in its scope, so downstream-app never makes it into the token
        AccessTokenResponse hop1 = assertExchangeSucceeds(AGENT_CLIENT_ID, AGENT_CLIENT_SECRET, loginRes.getAccessToken(), 1, null);
        AccessToken agentToken = oauth.verifyToken(hop1.getAccessToken());
        Assertions.assertTrue(agentToken.hasAudience(MCP_CLIENT_ID));
        Assertions.assertFalse(agentToken.hasAudience(DOWNSTREAM_CLIENT_ID));

        String actorToken = clientCredentialsToken(DOWNSTREAM_CLIENT_ID, DOWNSTREAM_CLIENT_SECRET);
        AccessTokenResponse res = exchange(DOWNSTREAM_CLIENT_ID, DOWNSTREAM_CLIENT_SECRET, hop1.getAccessToken(), actorToken);
        Assertions.assertFalse(res.isSuccess(), "Token exchange should have been rejected");
        Assertions.assertEquals(OAuthErrorException.ACCESS_DENIED, res.getError());
        Assertions.assertEquals("Client is not within the token audience", res.getErrorDescription());

        EventAssertion.assertError(events.poll())
                .type(EventType.TOKEN_EXCHANGE_ERROR)
                .clientId(DOWNSTREAM_CLIENT_ID)
                .error(Errors.NOT_ALLOWED)
                .details(Details.REASON, "client is not within the token audience");

        logout(loginRes.getRefreshToken());
    }

    @Test
    public void mostRestrictivePolicyWins() {
        realm.updateWithCleanup(r -> r
                .clientProfile(ClientProfileBuilder.create().name("chain")
                        .executor(ClientDelegationExecutorFactory.PROVIDER_ID, chainingEnabled()).build())
                .clientProfile(ClientProfileBuilder.create().name("no-chain")
                        .executor(ClientDelegationExecutorFactory.PROVIDER_ID, config(c -> c.setAllowChaining(false))).build())
                .clientPolicy(ClientPolicyBuilder.create().name("chain-policy")
                        .condition(AnyClientConditionFactory.PROVIDER_ID, null).profile("chain").build())
                .clientPolicy(ClientPolicyBuilder.create().name("no-chain-policy")
                        .condition(AnyClientConditionFactory.PROVIDER_ID, null).profile("no-chain").build()));

        AccessTokenResponse loginRes = login(AGENT_DELEGATION_SCOPE);

        // the first hop is not chaining, so neither policy has anything to say about it
        AccessTokenResponse hop1 = assertExchangeSucceeds(AGENT_CLIENT_ID, AGENT_CLIENT_SECRET, loginRes.getAccessToken(), MCP_CLIENT_ID, 1, null);

        // one policy allows chaining and the other denies it, so the denial wins whichever order they run in
        assertExchangeRejected(MCP_CLIENT_ID, MCP_CLIENT_SECRET, hop1.getAccessToken(),
                "Chaining an already delegated token is not allowed");

        logout(loginRes.getRefreshToken());
    }

    @Test
    public void emptyExecutorConfigPlusPolicyWithAllowChainingTrueMustStillReject() {
        realm.updateWithCleanup(r -> r
                .clientProfile(ClientProfileBuilder.create().name("no-config")
                        .executor(ClientDelegationExecutorFactory.PROVIDER_ID, new ClientDelegationExecutor.Configuration()).build())
                .clientProfile(ClientProfileBuilder.create().name("allow-chain")
                        .executor(ClientDelegationExecutorFactory.PROVIDER_ID, chainingEnabled()).build())
                .clientPolicy(ClientPolicyBuilder.create().name("no-config-policy")
                        .condition(AnyClientConditionFactory.PROVIDER_ID, null).profile("no-config").build())
                .clientPolicy(ClientPolicyBuilder.create().name("allow-chain-policy")
                        .condition(AnyClientConditionFactory.PROVIDER_ID, null).profile("allow-chain").build()));

        AccessTokenResponse loginRes = login(AGENT_DELEGATION_SCOPE);

        // the first hop is not chaining, so neither policy has anything to say about it
        AccessTokenResponse hop1 = assertExchangeSucceeds(AGENT_CLIENT_ID, AGENT_CLIENT_SECRET, loginRes.getAccessToken(), MCP_CLIENT_ID, 1, null);

        // one policy has empty config (defaults to false) and the other allows chaining, so the restrictive one wins
        assertExchangeRejected(MCP_CLIENT_ID, MCP_CLIENT_SECRET, hop1.getAccessToken(),
                "Chaining an already delegated token is not allowed");

        logout(loginRes.getRefreshToken());
    }

    @Test
    public void plainExchangeUnaffectedByChainPolicy() {
        addChainPolicy(chainingEnabled());

        AccessTokenResponse loginRes = login(null);

        // no actor_token, so this is an ordinary exchange and the delegation executor never fires
        AccessTokenResponse res = oauth.client(AGENT_CLIENT_ID, AGENT_CLIENT_SECRET).scope(null)
                .tokenExchangeRequest(loginRes.getAccessToken()).send();
        Assertions.assertTrue(res.isSuccess(), res.getError() + " - " + res.getErrorDescription());
        Assertions.assertNull(oauth.verifyToken(res.getAccessToken()).getOtherClaims().get(ACT));

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.TOKEN_EXCHANGE)
                .clientId(AGENT_CLIENT_ID)
                .details(Details.DELEGATION_CHAIN_DEPTH, null);

        logout(loginRes.getRefreshToken());
    }

    @Test
    // un-ignore once the downscope executor stops filtering the client itself out of its own scope containers
    @Disabled("Blocked by the restricted scopes bug in DefaultClientSessionContext, see https://github.com/keycloak/keycloak/issues/53280")
    public void downscopeEnforcerStopsScopeEscalationMidChain() {
        realm.updateWithCleanup(r -> r
                .clientProfile(ClientProfileBuilder.create().name("chain")
                        .executor(ClientDelegationExecutorFactory.PROVIDER_ID, chainingEnabled())
                        .executor(DownscopeAssertionGrantEnforcerExecutorFactory.PROVIDER_ID, null).build())
                .clientPolicy(ClientPolicyBuilder.create().name("chain-policy")
                        .condition(AnyClientConditionFactory.PROVIDER_ID, null).profile("chain").build()));

        AccessTokenResponse loginRes = login(AGENT_DELEGATION_SCOPE);

        // agent-app has no "email" scope, so the token it passes on carries none
        AccessTokenResponse hop1 = assertExchangeSucceeds(AGENT_CLIENT_ID, AGENT_CLIENT_SECRET, loginRes.getAccessToken(), MCP_CLIENT_ID, 1, null);
        Assertions.assertFalse(oauth.verifyToken(hop1.getAccessToken()).getScope().contains("email"));

        // the actor token is fetched first, as doing it inline would reset the scope on the shared client config
        String actorToken = clientCredentialsToken(MCP_CLIENT_ID, MCP_CLIENT_SECRET);
        AccessTokenResponse res = oauth.client(MCP_CLIENT_ID, MCP_CLIENT_SECRET).openid(false).scope("email")
                .tokenExchangeRequest(hop1.getAccessToken())
                .actorToken(actorToken)
                .actorTokenType(ACCESS_TOKEN_TYPE)
                .audience(DOWNSTREAM_CLIENT_ID)
                .send();
        oauth.openid(true).scope(null);

        Assertions.assertFalse(res.isSuccess(), "Token exchange should have been rejected");
        Assertions.assertEquals(OAuthErrorException.INVALID_SCOPE, res.getError());
        Assertions.assertTrue(res.getErrorDescription().startsWith("Scopes [email] not present in the initial access token"),
                res.getErrorDescription());
        events.poll();

        logout(loginRes.getRefreshToken());
    }

    // ===== Chain helpers =====

    private static ClientDelegationExecutor.Configuration config(Consumer<ClientDelegationExecutor.Configuration> customizer) {
        ClientDelegationExecutor.Configuration configuration = new ClientDelegationExecutor.Configuration();
        customizer.accept(configuration);
        return configuration;
    }

    /**
     * Chaining is off unless a policy turns it on, so every test that goes past the first hop needs this.
     */
    private static ClientDelegationExecutor.Configuration chainingEnabled() {
        return config(c -> c.setAllowChaining(true));
    }

    private AccessTokenResponse exchange(String clientId, String clientSecret, String subjectToken, String actorToken, String... audience) {
        return oauth.client(clientId, clientSecret).scope(null)
                .tokenExchangeRequest(subjectToken)
                .actorToken(actorToken)
                .actorTokenType(ACCESS_TOKEN_TYPE)
                .audience(audience)
                .send();
    }

    /**
     * A null expectedChain means the event must carry no chain detail at all, which is the case for a first hop.
     */
    private AccessTokenResponse assertExchangeSucceeds(String clientId, String clientSecret, String subjectToken,
            int expectedDepth, String expectedChain) {
        return assertExchangeSucceeds(clientId, clientSecret, subjectToken, null, expectedDepth, expectedChain);
    }

    /**
     * The next hop is named in the "audience" parameter, which narrows the delegated token down to it.
     */
    private AccessTokenResponse assertExchangeSucceeds(String clientId, String clientSecret, String subjectToken,
            String nextHop, int expectedDepth, String expectedChain) {
        String actorToken = clientCredentialsToken(clientId, clientSecret);
        AccessTokenResponse res = nextHop == null
                ? exchange(clientId, clientSecret, subjectToken, actorToken)
                : exchange(clientId, clientSecret, subjectToken, actorToken, nextHop);
        Assertions.assertTrue(res.isSuccess(), res.getError() + " - " + res.getErrorDescription());

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.TOKEN_EXCHANGE)
                .clientId(clientId)
                .details(Details.USERNAME, USERNAME)
                .details(Details.ACTOR_TYPE, Details.ACTOR_TYPE_CLIENT)
                .details(Details.ACTOR, clientId)
                .details(Details.DELEGATION_CHAIN_DEPTH, String.valueOf(expectedDepth))
                .details(Details.DELEGATION_CHAIN, expectedChain);
        return res;
    }

    private void assertExchangeRejected(String clientId, String clientSecret, String subjectToken, String expectedReason) {
        assertExchangeRejected(clientId, clientSecret, subjectToken, clientCredentialsToken(clientId, clientSecret), expectedReason);
    }

    private void assertExchangeRejected(String clientId, String clientSecret, String subjectToken, String actorToken, String expectedReason) {
        AccessTokenResponse res = exchange(clientId, clientSecret, subjectToken, actorToken);
        Assertions.assertFalse(res.isSuccess(), "Token exchange should have been rejected");
        Assertions.assertEquals(OAuthErrorException.INVALID_REQUEST, res.getError());
        Assertions.assertEquals(expectedReason, res.getErrorDescription());

        EventAssertion.assertError(events.poll())
                .type(EventType.TOKEN_EXCHANGE_ERROR)
                .clientId(clientId)
                .error(Errors.INVALID_TOKEN)
                .details(Details.REASON, expectedReason);
    }

    // ===== Login / token helpers =====

    private AccessTokenResponse login(String scope) {
        oauth.client(SUBJECT_CLIENT_ID, SUBJECT_CLIENT_SECRET);
        oauth.scope(scope).openLoginForm();
        oauth.fillLoginForm(USERNAME, PASSWORD);
        grantPage.assertCurrent();
        grantPage.accept();

        EventAssertion.assertSuccess(events.poll()).type(EventType.LOGIN).clientId(SUBJECT_CLIENT_ID);

        AccessTokenResponse res = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(res.isSuccess(), res.getError() + " - " + res.getErrorDescription());
        EventAssertion.assertSuccess(events.poll()).type(EventType.CODE_TO_TOKEN);
        return res;
    }

    private String clientCredentialsToken(String clientId, String clientSecret) {
        AccessTokenResponse res = oauth.client(clientId, clientSecret).scope(null)
                .doClientCredentialsGrantAccessTokenRequest();
        Assertions.assertTrue(res.isSuccess(), res.getError());
        EventAssertion.assertSuccess(events.poll()).type(EventType.CLIENT_LOGIN).clientId(clientId);
        return res.getAccessToken();
    }

    private String passwordGrantToken(String clientId, String clientSecret, String username) {
        AccessTokenResponse res = oauth.client(clientId, clientSecret).scope(null)
                .doPasswordGrantRequest(username, PASSWORD);
        Assertions.assertTrue(res.isSuccess(), res.getError());
        EventAssertion.assertSuccess(events.poll()).type(EventType.LOGIN).clientId(clientId);
        return res.getAccessToken();
    }

    private void logout(String refreshToken) {
        LogoutResponse logout = oauth.client(SUBJECT_CLIENT_ID, SUBJECT_CLIENT_SECRET).doLogout(refreshToken);
        Assertions.assertTrue(logout.isSuccess(), logout.getError() + " - " + logout.getErrorDescription());
    }

    // ===== Setup helpers =====

    private void addChainPolicy(ClientDelegationExecutor.Configuration configuration) {
        realm.updateWithCleanup(r -> r
                .clientProfile(ClientProfileBuilder.create().name("chain")
                        .executor(ClientDelegationExecutorFactory.PROVIDER_ID, configuration).build())
                .clientPolicy(ClientPolicyBuilder.create().name("chain-policy")
                        .condition(AnyClientConditionFactory.PROVIDER_ID, null).profile("chain").build()));
    }

    private void addHardcodedMapper(String clientId, ProtocolMapperRepresentation mapper) {
        AdminApiUtil.findClientByClientId(realm.admin(), clientId).getProtocolMappers().createMapper(List.of(mapper));
        realm.cleanup().add(r -> {
            ClientResource client = AdminApiUtil.findClientByClientId(r, clientId);
            client.getProtocolMappers().getMappers().stream()
                    .filter(m -> mapper.getName().equals(m.getName()))
                    .forEach(m -> client.getProtocolMappers().delete(m.getId()));
        });
    }

    /**
     * The single "delegate over all users" permission an admin would create, naming every actor that takes part.
     */
    private void addDelegatePermission() {
        ClientResource adminPerms = AdminApiUtil.findClientByClientId(realm.admin(), Constants.ADMIN_PERMISSIONS_CLIENT_ID);
        ClientPolicyRepresentation clientPolicy = PermissionTestUtils.createClientPolicy(realm, adminPerms,
                "Delegation Chain Client Policy", AGENT_CLIENT_ID, MCP_CLIENT_ID, DOWNSTREAM_CLIENT_ID);
        UserPolicyRepresentation userPolicy = PermissionTestUtils.createUserPolicy(realm, adminPerms,
                "Delegation Chain User Policy", userId(OTHER_USERNAME));

        // admin permissions are UNANIMOUS, so a second "delegate" permission would deny this one, all actors go into one policy
        AggregatePolicyRepresentation actors = PermissionTestUtils.createAggregatePolicy(realm, adminPerms,
                "Delegation Chain Actors", clientPolicy, userPolicy);
        addDelegatePermission(adminPerms, actors);
    }

    private void addDelegatePermission(ClientResource adminPerms, AbstractPolicyRepresentation policy) {
        ScopePermissionRepresentation permission = PermissionTestUtils.createAllPermission(adminPerms,
                AdminPermissionsSchema.USERS_RESOURCE_TYPE, policy, Set.of(AdminPermissionsSchema.DELEGATE));
        realm.cleanup().add(r -> {
            try {
                ClientResource perms = AdminApiUtil.findClientByClientId(r, Constants.ADMIN_PERMISSIONS_CLIENT_ID);
                perms.authorization().permissions().scope().findById(permission.getId()).remove();
            } catch (NotFoundException ignored) {
            }
        });
    }

    /**
     * The audience of a delegated token comes out of the ordinary role resolution, so every client that may be
     * targeted needs a role the subject user holds, and the caller needs that role in its scope.
     */
    private void addDelegationTargets() {
        createTargetRole(agentApp);
        createTargetRole(mcpApp);
        createTargetRole(downstreamApp);

        // agent-app may only ever reach mcp-app, mcp-app may reach agent-app and downstream-app
        allowTarget(agentApp.getId(), mcpApp);
        allowTarget(mcpApp.getId(), agentApp);
        allowTarget(mcpApp.getId(), downstreamApp);

        // test-app starts the chain, so its login token has to reach the client that takes the first hop
        String subjectClientId = AdminApiUtil.findClientByClientId(realm.admin(), SUBJECT_CLIENT_ID).toRepresentation().getId();
        allowTarget(subjectClientId, agentApp);
        allowTarget(subjectClientId, mcpApp);

        for (String username : List.of(USERNAME, OTHER_USERNAME)) {
            grantTargetRole(userId(username), agentApp);
            grantTargetRole(userId(username), mcpApp);
            grantTargetRole(userId(username), downstreamApp);
        }
    }

    private void createTargetRole(ManagedClient client) {
        RoleRepresentation role = new RoleRepresentation();
        role.setName(TARGET_ROLE);
        client.admin().roles().create(role);
        // removing the role takes its scope mappings and user grants with it
        realm.cleanup().add(r -> r.clients().get(client.getId()).roles().deleteRole(TARGET_ROLE));
    }

    private void allowTarget(String callerId, ManagedClient target) {
        realm.admin().clients().get(callerId).getScopeMappings().clientLevel(target.getId())
                .add(List.of(targetRole(target)));
    }

    private void grantTargetRole(String userId, ManagedClient target) {
        realm.admin().users().get(userId).roles().clientLevel(target.getId()).add(List.of(targetRole(target)));
    }

    private RoleRepresentation targetRole(ManagedClient target) {
        return target.admin().roles().get(TARGET_ROLE).toRepresentation();
    }

    private String userId(String username) {
        return AdminApiUtil.findUserByUsername(realm.admin(), username).getId();
    }

    private void removeDelegatePermissions() {
        ClientResource adminPerms = AdminApiUtil.findClientByClientId(realm.admin(), Constants.ADMIN_PERMISSIONS_CLIENT_ID);
        adminPerms.authorization().permissions().scope().findAll(null, null, null, "*", null, null).stream()
                .filter(p -> p.getScopesData() != null && p.getScopesData().stream()
                        .anyMatch(s -> AdminPermissionsSchema.DELEGATE.equals(s.getName())))
                .forEach(p -> adminPerms.authorization().permissions().scope().findById(p.getId()).remove());
    }

    // ===== Assertion helpers =====

    /**
     * Walks the nested "act" claim and checks it names the expected actors, most recent one first.
     */
    private void assertActChain(AccessToken token, String... expectedClientIds) {
        Map<String, Object> level = actClaim(token);
        for (String clientId : expectedClientIds) {
            Assertions.assertNotNull(level, "act level for " + clientId + " is missing");
            Assertions.assertEquals(clientId, level.get(CLIENT_ID), "act.client_id is not correct");
            Assertions.assertEquals(serviceAccountId(clientId), level.get(SUBJECT), "act.sub is not the service account of " + clientId);
            level = nestedAct(level);
        }
        Assertions.assertNull(level, "delegation chain is longer than expected");
    }

    private static int actorCount(AccessToken token) {
        int count = 0;
        for (Map<String, Object> level = actClaim(token); level != null; level = nestedAct(level)) {
            count++;
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> actClaim(AccessToken token) {
        Map<String, Object> act = (Map<String, Object>) token.getOtherClaims().get(ACT);
        Assertions.assertNotNull(act, "act claim should be present");
        return act;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> nestedAct(Map<String, Object> level) {
        Object nested = level.get(ACT);
        return nested instanceof Map ? (Map<String, Object>) nested : null;
    }

    private String serviceAccountId(String clientId) {
        return switch (clientId) {
            case AGENT_CLIENT_ID -> agentApp.admin().getServiceAccountUser().getId();
            case MCP_CLIENT_ID -> mcpApp.admin().getServiceAccountUser().getId();
            case DOWNSTREAM_CLIENT_ID -> downstreamApp.admin().getServiceAccountUser().getId();
            default -> throw new AssertionError("Unknown client " + clientId);
        };
    }

    // ===== Config classes =====

    /**
     * A delegation claim a protocol mapper wrote, which is the route that bypasses the delegation scope entirely.
     */
    static ProtocolMapperRepresentation hardcodedClaimMapper(String name, String claimName) {
        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
        mapper.setName(name);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        mapper.setProtocolMapper(HardcodedClaim.PROVIDER_ID);

        Map<String, String> config = new HashMap<>();
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, claimName);
        config.put(HardcodedClaim.CLAIM_VALUE, "hardcoded-actor");
        config.put(OIDCAttributeMapperHelper.JSON_TYPE, "String");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, Boolean.TRUE.toString());
        mapper.setConfig(config);
        return mapper;
    }

    /**
     * A forged "act" chain of the requested length, which is how a token reaches the depth limit without taking
     * the hops it would otherwise need, and with more actors than this realm has clients.
     */
    static ProtocolMapperRepresentation forgedActChainMapper(int actors) {
        // the innermost actor is the oldest one, so the chain is built from the inside out
        String chain = null;
        for (int i = 1; i <= actors; i++) {
            chain = "{\"" + SUBJECT + "\":\"forged-actor-" + i + "\",\"" + CLIENT_ID + "\":\"forged-client-" + i + "\""
                    + (chain == null ? "" : ",\"" + ACT + "\":" + chain) + "}";
        }

        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
        mapper.setName(HARDCODED_ACT_MAPPER);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        mapper.setProtocolMapper(HardcodedClaim.PROVIDER_ID);

        Map<String, String> config = new HashMap<>();
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, ACT);
        config.put(HardcodedClaim.CLAIM_VALUE, chain);
        config.put(OIDCAttributeMapperHelper.JSON_TYPE, "JSON");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, Boolean.TRUE.toString());
        mapper.setConfig(config);
        return mapper;
    }

    static ProtocolMapperRepresentation audienceMapper(String audience) {
        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
        mapper.setName("audience-" + audience);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        mapper.setProtocolMapper(AudienceProtocolMapper.PROVIDER_ID);

        Map<String, String> config = new HashMap<>();
        config.put(AudienceProtocolMapper.INCLUDED_CLIENT_AUDIENCE, audience);
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, Boolean.TRUE.toString());
        mapper.setConfig(config);
        return mapper;
    }

    static class ServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.PARAMETERIZED_SCOPES, Profile.Feature.TOKEN_EXCHANGE_DELEGATION);
        }
    }

    static class DelegationChainRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.adminPermissionsEnabled(true)
                    .users(UserBuilder.create(USERNAME).password(PASSWORD)
                                    .email("test@localhost").firstName("Test").lastName("User"),
                            UserBuilder.create(OTHER_USERNAME).password(PASSWORD)
                                    .email("otheruser@localhost").firstName("Other").lastName("User"));
        }
    }

    static class SubjectAppConfig extends DefaultOAuthClientConfiguration {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            // full scope off makes the scope mappings the real bound on where this token can be exchanged
            return super.configure(client)
                    .consentRequired(true)
                    .fullScopeEnabled(false);
        }
    }

    static class AgentAppConfig implements ClientConfig {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            // "roles" brings in the audience resolve mapper, which is what turns a scope mapping into an audience
            return client.clientId(AGENT_CLIENT_ID).name("AI Agent App").secret(AGENT_CLIENT_SECRET)
                    .serviceAccountsEnabled(true)
                    .fullScopeEnabled(false)
                    .defaultClientScopes("acr", "basic", "profile", "roles")
                    .attribute(OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_ENABLED, Boolean.TRUE.toString());
        }
    }

    static class McpAppConfig implements ClientConfig {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            // "email" is assigned here but not on agent-app, so a chain through agent-app can be seen to have dropped it
            return client.clientId(MCP_CLIENT_ID).name("MCP App").secret(MCP_CLIENT_SECRET)
                    .serviceAccountsEnabled(true)
                    .directAccessGrantsEnabled()
                    .fullScopeEnabled(false)
                    .defaultClientScopes("acr", "basic", "profile", "email", "roles")
                    .attribute(OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_ENABLED, Boolean.TRUE.toString());
        }
    }

    static class DownstreamAppConfig implements ClientConfig {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            // a client credentials token is not produced by an exchange, so lending one to mcp-app still needs a mapper
            return client.clientId(DOWNSTREAM_CLIENT_ID).name("Downstream App").secret(DOWNSTREAM_CLIENT_SECRET)
                    .serviceAccountsEnabled(true)
                    .fullScopeEnabled(false)
                    .defaultClientScopes("acr", "basic", "profile", "roles")
                    .attribute(OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_ENABLED, Boolean.TRUE.toString())
                    .protocolMappers(audienceMapper(MCP_CLIENT_ID));
        }
    }
}
