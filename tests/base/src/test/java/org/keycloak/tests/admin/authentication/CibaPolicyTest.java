package org.keycloak.tests.admin.authentication;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;

import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.CibaConfig;
import org.keycloak.representations.idm.CibaPolicyRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.tests.utils.admin.AdminEventPaths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class CibaPolicyTest extends AbstractPolicyTest {

    @AfterEach
    public void after() {
        CibaPolicyRepresentation rep = new CibaPolicyRepresentation();
        rep.setBackchannelTokenDeliveryMode(CibaConfig.DEFAULT_CIBA_POLICY_TOKEN_DELIVERY_MODE);
        rep.setInterval(CibaConfig.DEFAULT_CIBA_POLICY_INTERVAL);
        rep.setExpiresIn(CibaConfig.DEFAULT_CIBA_POLICY_EXPIRES_IN);
        rep.setAuthRequestedUserHint(CibaConfig.DEFAULT_CIBA_POLICY_AUTH_REQUESTED_USER_HINT);
        authMgmtResource.updateCibaPolicy(rep);
    }

    @Test
    public void testGetCibaPolicyReturnsDefaultPolicy() {
        CibaPolicyRepresentation result = authMgmtResource.getCibaPolicy();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(CibaConfig.DEFAULT_CIBA_POLICY_TOKEN_DELIVERY_MODE, result.getBackchannelTokenDeliveryMode());
        Assertions.assertEquals(CibaConfig.DEFAULT_CIBA_POLICY_INTERVAL, result.getInterval());
        Assertions.assertEquals(CibaConfig.DEFAULT_CIBA_POLICY_EXPIRES_IN, result.getExpiresIn());
        Assertions.assertEquals(CibaConfig.DEFAULT_CIBA_POLICY_AUTH_REQUESTED_USER_HINT, result.getAuthRequestedUserHint());
    }

    @Test
    public void testGetCibaPolicyReturnsAllFields() {
        CibaPolicyRepresentation result = authMgmtResource.getCibaPolicy();

        Assertions.assertNotNull(result.getBackchannelTokenDeliveryMode());
        Assertions.assertNotNull(result.getExpiresIn());
        Assertions.assertNotNull(result.getInterval());
        Assertions.assertNotNull(result.getAuthRequestedUserHint());
    }

    @Test
    public void testUpdateCibaPolicyPersistsAllFields() {
        CibaPolicyRepresentation rep = validCibaPolicy();
        rep.setBackchannelTokenDeliveryMode(CibaConfig.CIBA_SUPPORTED_MODES.get(1));
        rep.setExpiresIn(300);
        rep.setInterval(10);
        authMgmtResource.updateCibaPolicy(rep);

        CibaPolicyRepresentation result = authMgmtResource.getCibaPolicy();
        Assertions.assertEquals(rep.getBackchannelTokenDeliveryMode(), result.getBackchannelTokenDeliveryMode());
        Assertions.assertEquals(300, result.getExpiresIn());
        Assertions.assertEquals(10, result.getInterval());
        Assertions.assertEquals(rep.getAuthRequestedUserHint(), result.getAuthRequestedUserHint());
    }

    @Test
    public void testUpdateCibaPolicyAllSupportedBackchannelTokenDeliveryModes() {
        for (String mode : CibaConfig.CIBA_SUPPORTED_MODES) {
            CibaPolicyRepresentation rep = validCibaPolicy();
            rep.setBackchannelTokenDeliveryMode(mode);
            authMgmtResource.updateCibaPolicy(rep);

            Assertions.assertEquals(mode,
                    authMgmtResource.getCibaPolicy().getBackchannelTokenDeliveryMode(),
                    "Failed for backchannelTokenDeliveryMode: " + mode);
        }
    }

    @Test
    public void testUpdateCibaPolicyFailsOnUnsupportedBackchannelTokenDeliveryMode() {
        CibaPolicyRepresentation rep = validCibaPolicy();
        rep.setBackchannelTokenDeliveryMode("unsupported");
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateCibaPolicy(rep));
    }

    @Test
    public void testUpdateCibaPolicyFailsOnNullBackchannelTokenDeliveryMode() {
        CibaPolicyRepresentation rep = validCibaPolicy();
        rep.setBackchannelTokenDeliveryMode(null);
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateCibaPolicy(rep));
    }

    @Test
    public void testUpdateCibaPolicyFailsOnEmptyBackchannelTokenDeliveryMode() {
        CibaPolicyRepresentation rep = validCibaPolicy();
        rep.setBackchannelTokenDeliveryMode("");
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateCibaPolicy(rep));
    }

    @Test
    public void testUpdateCibaPolicyExpiresInAtLowerBoundary() {
        CibaPolicyRepresentation rep = validCibaPolicy();
        rep.setExpiresIn(10);
        authMgmtResource.updateCibaPolicy(rep);

        Assertions.assertEquals(10, authMgmtResource.getCibaPolicy().getExpiresIn());
    }

    @Test
    public void testUpdateCibaPolicyExpiresInAtUpperBoundary() {
        CibaPolicyRepresentation rep = validCibaPolicy();
        rep.setExpiresIn(600);
        authMgmtResource.updateCibaPolicy(rep);

        Assertions.assertEquals(600, authMgmtResource.getCibaPolicy().getExpiresIn());
    }

    @Test
    public void testUpdateCibaPolicyFailsOnExpiresInBelowMinimum() {
        CibaPolicyRepresentation rep = validCibaPolicy();
        rep.setExpiresIn(9);
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateCibaPolicy(rep));
    }

    @Test
    public void testUpdateCibaPolicyFailsOnExpiresInOfZero() {
        CibaPolicyRepresentation rep = validCibaPolicy();
        rep.setExpiresIn(0);
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateCibaPolicy(rep));
    }

    @Test
    public void testUpdateCibaPolicyFailsOnNegativeExpiresIn() {
        CibaPolicyRepresentation rep = validCibaPolicy();
        rep.setExpiresIn(-1);
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateCibaPolicy(rep));
    }

    @Test
    public void testUpdateCibaPolicyFailsOnExpiresInAboveMaximum() {
        CibaPolicyRepresentation rep = validCibaPolicy();
        rep.setExpiresIn(601);
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateCibaPolicy(rep));
    }

    @Test
    public void testUpdateCibaPolicyIntervalAtLowerBoundary() {
        CibaPolicyRepresentation rep = validCibaPolicy();
        rep.setInterval(0);
        authMgmtResource.updateCibaPolicy(rep);

        Assertions.assertEquals(0, authMgmtResource.getCibaPolicy().getInterval());
    }

    @Test
    public void testUpdateCibaPolicyIntervalAtUpperBoundary() {
        CibaPolicyRepresentation rep = validCibaPolicy();
        rep.setInterval(600);
        authMgmtResource.updateCibaPolicy(rep);

        Assertions.assertEquals(600, authMgmtResource.getCibaPolicy().getInterval());
    }

    @Test
    public void testUpdateCibaPolicyFailsOnNegativeInterval() {
        CibaPolicyRepresentation rep = validCibaPolicy();
        rep.setInterval(-1);
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateCibaPolicy(rep));
    }

    @Test
    public void testUpdateCibaPolicyFailsOnIntervalAboveMaximum() {
        CibaPolicyRepresentation rep = validCibaPolicy();
        rep.setInterval(601);
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateCibaPolicy(rep));
    }

    @Test
    public void testUpdateCibaPolicyFailsOnUnsupportedAuthRequestedUserHint() {
        CibaPolicyRepresentation rep = validCibaPolicy();
        rep.setAuthRequestedUserHint("id_token_hint");
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateCibaPolicy(rep));
    }

    @Test
    public void testUpdateCibaPolicyFailsOnEmptyAuthRequestedUserHint() {
        CibaPolicyRepresentation rep = validCibaPolicy();
        rep.setAuthRequestedUserHint("");
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateCibaPolicy(rep));
    }

    @Test
    public void testUpdateCibaPolicyFailsOnNullAuthRequestedUserHint() {
        CibaPolicyRepresentation rep = validCibaPolicy();
        rep.setAuthRequestedUserHint(null);
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateCibaPolicy(rep));
    }

    @Test
    public void testUpdateCibaPolicyOverwritesPreviousPolicy() {
        CibaPolicyRepresentation first = validCibaPolicy();
        first.setExpiresIn(100);
        authMgmtResource.updateCibaPolicy(first);

        CibaPolicyRepresentation second = validCibaPolicy();
        second.setExpiresIn(200);
        authMgmtResource.updateCibaPolicy(second);

        Assertions.assertEquals(200, authMgmtResource.getCibaPolicy().getExpiresIn());
    }

    @Test
    public void testUpdateCibaPolicyIsIdempotent() {
        CibaPolicyRepresentation rep = validCibaPolicy();
        authMgmtResource.updateCibaPolicy(rep);
        authMgmtResource.updateCibaPolicy(rep);

        CibaPolicyRepresentation result = authMgmtResource.getCibaPolicy();
        Assertions.assertEquals(rep.getExpiresIn(), result.getExpiresIn());
        Assertions.assertEquals(rep.getInterval(), result.getInterval());
    }

    @Test
    public void testGetCibaPolicyRequiresViewRealmPermission() {
        Assertions.assertThrows(
                ForbiddenException.class,
                () -> clients.get("view-users").realm(managedRealm.getId()).flows().getCibaPolicy()
        );
        Assertions.assertDoesNotThrow(
                () -> clients.get("view-realm").realm(managedRealm.getId()).flows().getCibaPolicy()
        );
    }

    @Test
    public void testUpdateCibaPolicyRequiresManageRealmPermission() {
        Assertions.assertThrows(
                ForbiddenException.class,
                () -> clients.get("view-realm").realm(managedRealm.getId()).flows().updateCibaPolicy(validCibaPolicy())
        );
        Assertions.assertDoesNotThrow(
                () -> clients.get("manage-realm").realm(managedRealm.getId()).flows().updateCibaPolicy(validCibaPolicy())
        );
    }

    @Test
    public void testUpdateOTPPolicySuccessTriggersAdminEvent() {
        authMgmtResource.updateCibaPolicy(validCibaPolicy());

        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.UPDATE)
                .resourceType(ResourceType.AUTHENTICATION_POLICY)
                .resourcePath(AdminEventPaths.cibaPolicyPath())
                .representation(validCibaPolicy());
    }

    @Test
    public void testUpdateOTPPolicyFailureDoesNotTriggerAdminEvent() {
        CibaPolicyRepresentation rep = new CibaPolicyRepresentation();

        Assertions.assertThrows(
                BadRequestException.class,
                () -> authMgmtResource.updateCibaPolicy(rep)
        );

        Assertions.assertNull(adminEvents.poll());
    }

    private CibaPolicyRepresentation validCibaPolicy() {
        CibaPolicyRepresentation rep = new CibaPolicyRepresentation();
        rep.setBackchannelTokenDeliveryMode(CibaConfig.CIBA_SUPPORTED_MODES.get(0));
        rep.setExpiresIn(120);
        rep.setInterval(5);
        rep.setAuthRequestedUserHint(CibaConfig.DEFAULT_CIBA_POLICY_AUTH_REQUESTED_USER_HINT);
        return rep;
    }
}
