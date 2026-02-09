package org.keycloak.tests.admin.realm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.BrowserSecurityHeaders;
import org.keycloak.models.CibaConfig;
import org.keycloak.models.OAuth2DeviceConfig;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.ParConfig;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.models.cache.infinispan.RealmAdapter;
import org.keycloak.models.jpa.entities.RealmAttributes;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;

import com.google.common.collect.Sets;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class RealmAttributesTest extends AbstractRealmTest {

    /**
     * Checks attributes exposed as fields are not also included as attributes
     */
    @Test
    public void excludesFieldsFromAttributes() {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setRealm("attributes");

        adminClient.realms().create(rep);

        RealmRepresentation rep2 = adminClient.realm("attributes").toRepresentation();
        if (rep2.getAttributes() != null) {
            Stream.of(CibaConfig.CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE,
                    CibaConfig.CIBA_EXPIRES_IN,
                    CibaConfig.CIBA_INTERVAL,
                    CibaConfig.CIBA_AUTH_REQUESTED_USER_HINT).forEach(i -> rep2.getAttributes().remove(i));
        }

        Set<String> attributesKeys = rep2.getAttributes().keySet();

        int expectedAttributesCount = 3;
        final Set<String> expectedAttributes = Sets.newHashSet(
                OAuth2DeviceConfig.OAUTH2_DEVICE_CODE_LIFESPAN,
                OAuth2DeviceConfig.OAUTH2_DEVICE_POLLING_INTERVAL,
                ParConfig.PAR_REQUEST_URI_LIFESPAN
        );

        // This attribute is represented in Legacy store as attribute and for Map store as a field
        expectedAttributes.add(OTPPolicy.REALM_REUSABLE_CODE_ATTRIBUTE);
        expectedAttributesCount++;

        assertThat(attributesKeys.size(), CoreMatchers.is(expectedAttributesCount));
        assertThat(attributesKeys, CoreMatchers.is(expectedAttributes));

        adminClient.realms().realm("attributes").remove();
    }

    /**
     * Checks attributes exposed as fields are not deleted on update realm
     */
    @Test
    public void testFieldNotErased() {
        Long dummyLong = 999L;
        Integer dummyInt = 999;
        Map<String, String> browserSecurityHeaders = new HashMap<>(Arrays.stream(
                BrowserSecurityHeaders.values()).collect(Collectors.toMap(
                BrowserSecurityHeaders::getKey,
                headerValue -> headerValue.getDefaultValue().isBlank()
                        ? "non-null-to-test"
                        : headerValue.getDefaultValue()
        )));

        RealmRepresentation rep = new RealmRepresentation();
        rep.setRealm("attributes");
        rep.setDisplayName("DISPLAY_NAME");
        rep.setDisplayNameHtml("DISPLAY_NAME_HTML");
        rep.setDefaultSignatureAlgorithm("RS256");
        rep.setBruteForceProtected(true);
        rep.setPermanentLockout(true);
        rep.setMaxFailureWaitSeconds(dummyInt);
        rep.setWaitIncrementSeconds(dummyInt);
        rep.setQuickLoginCheckMilliSeconds(dummyLong);
        rep.setMinimumQuickLoginWaitSeconds(dummyInt);
        rep.setMaxDeltaTimeSeconds(dummyInt);
        rep.setFailureFactor(dummyInt);
        rep.setActionTokenGeneratedByAdminLifespan(dummyInt);
        rep.setActionTokenGeneratedByUserLifespan(dummyInt);
        rep.setOfflineSessionMaxLifespanEnabled(true);
        rep.setOfflineSessionMaxLifespan(dummyInt);
        rep.setBrowserSecurityHeaders(browserSecurityHeaders);

        rep.setWebAuthnPolicyRpEntityName("RP_ENTITY_NAME");
        rep.setWebAuthnPolicySignatureAlgorithms(Collections.singletonList("RS256"));
        rep.setWebAuthnPolicyRpId("localhost");
        rep.setWebAuthnPolicyAttestationConveyancePreference("Direct");
        rep.setWebAuthnPolicyAuthenticatorAttachment("Platform");
        rep.setWebAuthnPolicyRequireResidentKey("Yes");
        rep.setWebAuthnPolicyUserVerificationRequirement("Required");
        rep.setWebAuthnPolicyCreateTimeout(dummyInt);
        rep.setWebAuthnPolicyAvoidSameAuthenticatorRegister(true);
        rep.setWebAuthnPolicyAcceptableAaguids(Collections.singletonList("00000000-0000-0000-0000-000000000000"));

        rep.setWebAuthnPolicyPasswordlessRpEntityName("RP_ENTITY_NAME");
        rep.setWebAuthnPolicyPasswordlessSignatureAlgorithms(Collections.singletonList("RS256"));
        rep.setWebAuthnPolicyPasswordlessRpId("localhost");
        rep.setWebAuthnPolicyPasswordlessAttestationConveyancePreference("Direct");
        rep.setWebAuthnPolicyPasswordlessAuthenticatorAttachment("Platform");
        rep.setWebAuthnPolicyPasswordlessRequireResidentKey("Yes");
        rep.setWebAuthnPolicyPasswordlessUserVerificationRequirement("Required");
        rep.setWebAuthnPolicyPasswordlessCreateTimeout(dummyInt);
        rep.setWebAuthnPolicyPasswordlessAvoidSameAuthenticatorRegister(true);
        rep.setWebAuthnPolicyPasswordlessAcceptableAaguids(Collections.singletonList("00000000-0000-0000-0000-000000000000"));

        adminClient.realms().create(rep);

        RealmRepresentation rep2 = new RealmRepresentation();
        rep2.setAttributes(Collections.singletonMap("frontendUrl", "http://localhost/frontEnd"));
        adminClient.realm("attributes").update(rep2);

        rep = adminClient.realm("attributes").toRepresentation();
        assertEquals("DISPLAY_NAME", rep.getDisplayName());
        assertEquals("DISPLAY_NAME_HTML", rep.getDisplayNameHtml());
        assertEquals("RS256", rep.getDefaultSignatureAlgorithm());
        assertTrue(rep.isBruteForceProtected());
        assertTrue(rep.isPermanentLockout());
        assertEquals(dummyInt, rep.getMaxFailureWaitSeconds());
        assertEquals(dummyInt, rep.getWaitIncrementSeconds());
        assertEquals(dummyLong, rep.getQuickLoginCheckMilliSeconds());
        assertEquals(dummyInt, rep.getMinimumQuickLoginWaitSeconds());
        assertEquals(dummyInt, rep.getMaxDeltaTimeSeconds());
        assertEquals(dummyInt, rep.getFailureFactor());
        assertEquals(dummyInt, rep.getActionTokenGeneratedByAdminLifespan());
        assertEquals(dummyInt, rep.getActionTokenGeneratedByUserLifespan());
        assertTrue(rep.getOfflineSessionMaxLifespanEnabled());
        assertEquals(dummyInt, rep.getOfflineSessionMaxLifespan());

        assertEquals("RP_ENTITY_NAME", rep.getWebAuthnPolicyRpEntityName());
        assertEquals(Collections.singletonList("RS256"), rep.getWebAuthnPolicySignatureAlgorithms());
        assertEquals("localhost", rep.getWebAuthnPolicyRpId());
        assertEquals("Direct", rep.getWebAuthnPolicyAttestationConveyancePreference());
        assertEquals("Platform", rep.getWebAuthnPolicyAuthenticatorAttachment());
        assertEquals("Yes", rep.getWebAuthnPolicyRequireResidentKey());
        assertEquals("Required", rep.getWebAuthnPolicyUserVerificationRequirement());
        assertEquals(dummyInt, rep.getWebAuthnPolicyCreateTimeout());
        assertTrue(rep.isWebAuthnPolicyAvoidSameAuthenticatorRegister());
        assertEquals(Collections.singletonList("00000000-0000-0000-0000-000000000000"), rep.getWebAuthnPolicyAcceptableAaguids());

        assertEquals("RP_ENTITY_NAME", rep.getWebAuthnPolicyPasswordlessRpEntityName());
        assertEquals(Collections.singletonList("RS256"), rep.getWebAuthnPolicyPasswordlessSignatureAlgorithms());
        assertEquals("localhost", rep.getWebAuthnPolicyPasswordlessRpId());
        assertEquals("Direct", rep.getWebAuthnPolicyPasswordlessAttestationConveyancePreference());
        assertEquals("Platform", rep.getWebAuthnPolicyPasswordlessAuthenticatorAttachment());
        assertEquals("Yes", rep.getWebAuthnPolicyPasswordlessRequireResidentKey());
        assertEquals("Required", rep.getWebAuthnPolicyPasswordlessUserVerificationRequirement());
        assertEquals(dummyInt, rep.getWebAuthnPolicyPasswordlessCreateTimeout());
        assertTrue(rep.isWebAuthnPolicyPasswordlessAvoidSameAuthenticatorRegister());
        assertEquals(Collections.singletonList("00000000-0000-0000-0000-000000000000"), rep.getWebAuthnPolicyPasswordlessAcceptableAaguids());

        assertEquals(browserSecurityHeaders, rep.getBrowserSecurityHeaders());

        adminClient.realms().realm("attributes").remove();
    }

    @Test
    public void updateRealmAttributes() {
        // first change
        RealmRepresentation rep = new RealmRepresentation();
        List<String> webAuthnPolicyAcceptableAaguids = new ArrayList<>();
        webAuthnPolicyAcceptableAaguids.add("aaguid1");
        webAuthnPolicyAcceptableAaguids.add("aaguid2");

        rep.setAttributes(new HashMap<>());
        rep.getAttributes().put("foo1", "bar1");
        rep.getAttributes().put("foo2", "bar2");

        rep.setWebAuthnPolicyRpEntityName("keycloak");
        rep.setWebAuthnPolicyAcceptableAaguids(webAuthnPolicyAcceptableAaguids);
        rep.setBruteForceProtected(true);
        rep.setDisplayName("dn1");

        managedRealm.admin().update(rep);
        AdminEventAssertion.assertSuccess(adminEvents.poll()).operationType(OperationType.UPDATE).resourceType(ResourceType.REALM);

        rep = managedRealm.admin().toRepresentation();
        assertEquals("bar1", rep.getAttributes().get("foo1"));
        assertEquals("bar2", rep.getAttributes().get("foo2"));
        assertTrue(rep.isBruteForceProtected());
        assertEquals("dn1", rep.getDisplayName());
        assertEquals(webAuthnPolicyAcceptableAaguids, rep.getWebAuthnPolicyAcceptableAaguids());

        // second change
        webAuthnPolicyAcceptableAaguids.clear();
        rep.setBruteForceProtected(false);
        rep.setDisplayName("dn2");
        rep.getAttributes().put("foo1", "bar11");
        rep.getAttributes().remove("foo2");
        rep.setWebAuthnPolicyAcceptableAaguids(webAuthnPolicyAcceptableAaguids);

        managedRealm.admin().update(rep);
        AdminEventAssertion.assertSuccess(adminEvents.poll()).operationType(OperationType.UPDATE).resourceType(ResourceType.REALM);

        rep = managedRealm.admin().toRepresentation();

        assertFalse(rep.isBruteForceProtected());
        assertEquals("dn2", rep.getDisplayName());

        assertEquals("bar11", rep.getAttributes().get("foo1"));
        assertFalse(rep.getAttributes().containsKey("foo2"));
        assertTrue(rep.getWebAuthnPolicyAcceptableAaguids().isEmpty());
    }

    @Test
    public void testSetEmptyAttributeValues() {
        String realmName = "testSetEmptyAttributeValues";
        RealmRepresentation rep = new RealmRepresentation();
        rep.setRealm(realmName);
        rep.setAttributes(new HashMap<>());
        rep.getAttributes().put("myboolean", "");
        rep.getAttributes().put("mylong", "");
        rep.getAttributes().put("myint", "");
        rep.getAttributes().put(RealmAttributes.ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN + ".something", "");

        adminClient.realms().create(rep);

        try {
            adminClient.realm(realmName);

            runOnServer.run(session -> {
                RealmModel realm = session.realms().getRealmByName(realmName);
                Assertions.assertInstanceOf(RealmAdapter.class, realm);

                Assertions.assertNull(realm.getUserActionTokenLifespans().get("something"));
                Assertions.assertEquals(true, realm.getAttribute("myboolean", true));
                Assertions.assertEquals(realm.getAttribute("mylong", (long) 123), Long.valueOf(123));
                Assertions.assertEquals(realm.getAttribute("myint", 1234), Integer.valueOf(1234));

                RealmProvider delegate = session.getProvider(CacheRealmProvider.class).getRealmDelegate();
                RealmModel realm2 = delegate.getRealm(realm.getId());
                Assertions.assertInstanceOf(org.keycloak.models.jpa.RealmAdapter.class, realm2);

                Assertions.assertNull(realm2.getUserActionTokenLifespans().get("something"));
                Assertions.assertEquals(true, realm2.getAttribute("myboolean", true));
                Assertions.assertEquals(realm2.getAttribute("mylong", (long) 123), Long.valueOf(123));
                Assertions.assertEquals(realm2.getAttribute("myint", 1234), Integer.valueOf(1234));
            });
        } finally {
            adminClient.realm(realmName).remove();
        }
    }
}
