package org.keycloak.testsuite.authz;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.client.resource.PermissionResource;
import org.keycloak.forms.account.freemarker.model.AuthorizationBean;
import org.keycloak.forms.account.freemarker.model.AuthorizationBean.ResourceBean;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.*;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

import java.util.List;

@AuthServerContainerExclude(AuthServer.REMOTE)
public class UmaRepresentationTest extends AbstractResourceServerTest {
    private ResourceRepresentation resource;
    private PermissionResource permission;

    private void createPermissionTicket() {
        PermissionTicketRepresentation ticket = new PermissionTicketRepresentation();
        ticket.setOwner(resource.getOwner().getId());
        ticket.setResource(resource.getId());
        ticket.setRequesterName("kolo");
        ticket.setScopeName("ScopeA");
        ticket.setGranted(true);
        permission.create(ticket);
    }

    @Test
    public void testCanRepresentPermissionTicketWithNamesOfResourceOwnedByUser() throws Exception {
        resource = addResource("Resource A", "marta", true, "ScopeA");
        permission = getAuthzClient().protection("marta", "password").permission();
        createPermissionTicket();

        List<PermissionTicketRepresentation> permissionTickets = permission.find(resource.getId(), null, null, null, null, true, null, null);
        Assert.assertFalse(permissionTickets.isEmpty());
        Assert.assertEquals(1, permissionTickets.size());

        PermissionTicketRepresentation ticket = permissionTickets.get(0);
        Assert.assertEquals(ticket.getOwnerName(), "marta");
        Assert.assertEquals(ticket.getRequesterName(), "kolo");
        Assert.assertEquals(ticket.getResourceName(), "Resource A");
        Assert.assertEquals(ticket.getScopeName(), "ScopeA");
        Assert.assertTrue(ticket.isGranted());
    }

    @Test
    public void testCanRepresentPermissionTicketWithNamesOfResourceOwnedByClient() throws Exception {
        resource = addResource("Resource A", getClient(getRealm()).toRepresentation().getId(), true, "ScopeA");
        permission = getAuthzClient().protection().permission();
        createPermissionTicket();

        List<PermissionTicketRepresentation> permissionTickets = permission.find(resource.getId(), null, null, null, null, true, null, null);
        Assert.assertFalse(permissionTickets.isEmpty());
        Assert.assertEquals(1, permissionTickets.size());

        PermissionTicketRepresentation ticket = permissionTickets.get(0);
        Assert.assertEquals(ticket.getOwnerName(), "resource-server-test");
        Assert.assertEquals(ticket.getRequesterName(), "kolo");
        Assert.assertEquals(ticket.getResourceName(), "Resource A");
        Assert.assertEquals(ticket.getScopeName(), "ScopeA");
        Assert.assertTrue(ticket.isGranted());
    }

    @Test
    public void testCanRepresentPolicyResultGrantOfResourceOwnedByUser() throws Exception {
        resource = addResource("Resource A", "marta", true, "ScopeA");
        permission = getAuthzClient().protection("marta", "password").permission();
        createPermissionTicket();

        RealmResource realm = getRealm();
        String resourceServerId = getClient(realm).toRepresentation().getId();
        UserRepresentation user = realm.users().search("kolo").get(0);

        PolicyEvaluationRequest request = new PolicyEvaluationRequest();
        request.setUserId(user.getId());
        request.setClientId(resourceServerId);
        request.addResource("Resource A", "ScopeA");
        PolicyEvaluationResponse result = getClient(realm).authorization().policies().evaluate(request);
        Assert.assertEquals(result.getStatus(), DecisionEffect.PERMIT);

        List<PolicyEvaluationResponse.EvaluationResultRepresentation> evaluations = result.getResults();
        Assert.assertFalse(evaluations.isEmpty());
        Assert.assertEquals(1, evaluations.size());

        List<PolicyEvaluationResponse.PolicyResultRepresentation> policies = evaluations.get(0).getPolicies();
        Assert.assertFalse(evaluations.isEmpty());
        Assert.assertEquals(1, evaluations.size());

        String description = policies.get(0).getPolicy().getDescription();
        Assert.assertTrue(description.startsWith("Resource owner (marta) grants access"));
    }

    @Test
    public void testCanRepresentPolicyResultGrantOfResourceOwnedByClient() throws Exception {
        resource = addResource("Resource A", getClient(getRealm()).toRepresentation().getId(), true, "ScopeA");
        permission = getAuthzClient().protection().permission();
        createPermissionTicket();

        RealmResource realm = getRealm();
        String resourceServerId = getClient(realm).toRepresentation().getId();
        UserRepresentation user = realm.users().search("kolo").get(0);

        PolicyEvaluationRequest request = new PolicyEvaluationRequest();
        request.setUserId(user.getId());
        request.setClientId(resourceServerId);
        request.addResource("Resource A", "ScopeA");
        PolicyEvaluationResponse result = getClient(realm).authorization().policies().evaluate(request);
        Assert.assertEquals(result.getStatus(), DecisionEffect.PERMIT);

        List<PolicyEvaluationResponse.EvaluationResultRepresentation> evaluations = result.getResults();
        Assert.assertFalse(evaluations.isEmpty());
        Assert.assertEquals(1, evaluations.size());

        List<PolicyEvaluationResponse.PolicyResultRepresentation> policies = evaluations.get(0).getPolicies();
        Assert.assertFalse(evaluations.isEmpty());
        Assert.assertEquals(1, evaluations.size());

        String description = policies.get(0).getPolicy().getDescription();
        Assert.assertTrue(description.startsWith("Resource owner (resource-server-test) grants access"));
    }

    @Test
    public void testCanRepresentResourceBeanOfResourceOwnedByUser() throws Exception {
        resource = addResource("Resource A", "marta", true, "ScopeA");
        testingClient.server().run(UmaRepresentationTest::testCanRepresentResourceBeanOfResourceOwnedByUser);
    }

    public static void testCanRepresentResourceBeanOfResourceOwnedByUser(KeycloakSession session) {
        session.getContext().setRealm(session.realms().getRealmByName("authz-test"));
        AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);

        AuthorizationBean authorizationBean  = new AuthorizationBean(session, null, session.getContext().getUri());
        ClientModel client = session.getContext().getRealm().getClientByClientId("resource-server-test");
        UserModel user = session.userStorageManager().getUserByUsername(session.getContext().getRealm(), "marta");
        ResourceBean resourceBean = authorizationBean.new ResourceBean(
            authorization.getStoreFactory().getResourceStore().findByName(
                "Resource A", user.getId(), client.getId()
            )
        );

        Assert.assertEquals("Resource A", resourceBean.getName());
        Assert.assertEquals("marta", resourceBean.getOwnerName());
        Assert.assertNotNull(resourceBean.getUserOwner());
        Assert.assertEquals("marta", resourceBean.getUserOwner().getUsername());
        Assert.assertNull(resourceBean.getClientOwner());
    }

    @Test
    public void testCanRepresentResourceBeanOfResourceOwnedByClient() throws Exception {
        resource = addResource("Resource A", getClient(getRealm()).toRepresentation().getId(), true, "ScopeA");
        testingClient.server().run(UmaRepresentationTest::testCanRepresentResourceBeanOfResourceOwnedByClient);
    }

    public static void testCanRepresentResourceBeanOfResourceOwnedByClient(KeycloakSession session) {
        session.getContext().setRealm(session.realms().getRealmByName("authz-test"));
        AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);

        AuthorizationBean authorizationBean  = new AuthorizationBean(session, null, session.getContext().getUri());
        ClientModel client = session.getContext().getRealm().getClientByClientId("resource-server-test");
        ResourceBean resourceBean = authorizationBean.new ResourceBean(
            authorization.getStoreFactory().getResourceStore().findByName(
                "Resource A", client.getId(), client.getId()
            )
        );

        Assert.assertEquals("Resource A", resourceBean.getName());
        Assert.assertEquals("resource-server-test", resourceBean.getOwnerName());
        Assert.assertNotNull(resourceBean.getClientOwner());
        Assert.assertEquals("resource-server-test", resourceBean.getClientOwner().getClientId());
        Assert.assertNull(resourceBean.getUserOwner());
    }
}
