package org.keycloak.performance.dataset.idm.authorization;

import java.util.List;
import javax.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.performance.dataset.idm.Client;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.performance.dataset.NestedEntity;
import org.keycloak.performance.dataset.Updatable;

/**
 *
 * @author tkyjovsk
 */
public class ResourceServer extends NestedEntity<Client, ResourceServerRepresentation>
        implements Updatable<ResourceServerRepresentation> {

    private List<Scope> scopes;
    private List<Resource> resources;
    private List<RolePolicy> rolePolicies;
    private List<JsPolicy> jsPolicies;
    private List<UserPolicy> userPolicies;
    private List<ClientPolicy> clientPolicies;
    private List<ResourcePermission> resourcePermissions;
    private List<ScopePermission> scopePermissions;

    private List<Policy> allPolicies;

    public ResourceServer(Client client) {
        super(client);
    }

    @Override
    public ResourceServerRepresentation newRepresentation() {
        return new ResourceServerRepresentation();
    }

    public Client getClient() {
        return getParentEntity();
    }

    @Override
    public ResourceServerRepresentation getRepresentation() {
        ResourceServerRepresentation r = super.getRepresentation();
        r.setId(getClient().getRepresentation().getId());
        r.setClientId(getClient().getRepresentation().getClientId());
        r.setName(getClient().getRepresentation().getName());
        return r;
    }

    @Override
    public String getId() {
        return getClient().getId();
    }

    @Override
    public String toString() {
        return getClient().toString();
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

    public List<Scope> getScopes() {
        return scopes;
    }

    public void setScopes(List<Scope> scopes) {
        this.scopes = scopes;
    }

    public List<RolePolicy> getRolePolicies() {
        return rolePolicies;
    }

    public void setRolePolicies(List<RolePolicy> rolePolicies) {
        this.rolePolicies = rolePolicies;
    }

    public AuthorizationResource resource(Keycloak adminClient) {
        return getClient().resource(adminClient).authorization();
    }

    @Override
    public void update(Keycloak adminClient) {
        resource(adminClient).update(getRepresentation());
    }

    @Override
    public void delete(Keycloak adminClient) {
        getClient().delete(adminClient);
    }

    public List<JsPolicy> getJsPolicies() {
        return jsPolicies;
    }

    public void setJsPolicies(List<JsPolicy> jsPolicies) {
        this.jsPolicies = jsPolicies;
    }

    public List<UserPolicy> getUserPolicies() {
        return userPolicies;
    }

    public void setUserPolicies(List<UserPolicy> userPolicies) {
        this.userPolicies = userPolicies;
    }

    public List<ClientPolicy> getClientPolicies() {
        return clientPolicies;
    }

    public void setClientPolicies(List<ClientPolicy> clientPolicies) {
        this.clientPolicies = clientPolicies;
    }

    public List<ResourcePermission> getResourcePermissions() {
        return resourcePermissions;
    }

    public void setResourcePermissions(List<ResourcePermission> resourcePermissions) {
        this.resourcePermissions = resourcePermissions;
    }

    public List<Policy> getAllPolicies() {
        return allPolicies;
    }

    public void setAllPolicies(List<Policy> allPolicies) {
        this.allPolicies = allPolicies;
    }

    public List<ScopePermission> getScopePermissions() {
        return scopePermissions;
    }

    public void setScopePermissions(List<ScopePermission> scopePermissions) {
        this.scopePermissions = scopePermissions;
    }

}
