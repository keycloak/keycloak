package org.keycloak.performance.dataset;

import java.util.Iterator;
import org.keycloak.performance.dataset.idm.Realm;
import java.util.List;
import java.util.stream.Stream;
import org.keycloak.performance.dataset.idm.Client;
import org.keycloak.performance.dataset.idm.ClientRole;
import org.keycloak.performance.dataset.idm.ClientRoleMappings;
import org.keycloak.performance.dataset.idm.Credential;
import org.keycloak.performance.dataset.idm.Group;
import org.keycloak.performance.dataset.idm.RealmRole;
import org.keycloak.performance.dataset.idm.RoleMappings;
import org.keycloak.performance.dataset.idm.User;
import org.keycloak.performance.dataset.idm.authorization.ClientPolicy;
import org.keycloak.performance.dataset.idm.authorization.JsPolicy;
import org.keycloak.performance.dataset.idm.authorization.Resource;
import org.keycloak.performance.dataset.idm.authorization.ResourcePermission;
import org.keycloak.performance.dataset.idm.authorization.ResourceServer;
import org.keycloak.performance.dataset.idm.authorization.RolePolicy;
import org.keycloak.performance.dataset.idm.authorization.Scope;
import org.keycloak.performance.dataset.idm.authorization.ScopePermission;
import org.keycloak.performance.dataset.idm.authorization.UserPolicy;
import org.keycloak.performance.iteration.RandomIterator;

/**
 *
 * @author tkyjovsk
 */
public class Dataset extends Entity<DatasetRepresentation> {

    private List<Realm> realms;

    private List<User> allUsers;
    private List<Client> allClients;

    @Override
    public DatasetRepresentation newRepresentation() {
        return new DatasetRepresentation();
    }

    @Override
    public String toString() {
        String s = getRepresentation().getName();
        return s == null || s.isEmpty() ? "dataset" : s;
    }

    public List<Realm> getRealms() {
        return realms;
    }

    public void setRealms(List<Realm> realms) {
        this.realms = realms;
    }

    public List<User> getAllUsers() {
        return allUsers;
    }

    public void setAllUsers(List<User> allUsers) {
        this.allUsers = allUsers;
    }

    public Iterator<User> randomUsersIterator() {
        return new RandomIterator<>(getAllUsers());
    }

    public List<Client> getAllClients() {
        return allClients;
    }

    public void setAllClients(List<Client> allClients) {
        this.allClients = allClients;
    }

    public Iterator<Realm> randomRealmIterator() {
        return new RandomIterator<>(getRealms());
    }

    public Stream<Realm> realms() {
        return getRealms().stream();
    }

    public Stream<RealmRole> realmRoles() {
        return getRealms().stream().map(Realm::getRealmRoles).flatMap(List::stream);
    }

    public Stream<Client> clients() {
        return getRealms().stream().map(Realm::getClients).flatMap(List::stream);
    }

    public Stream<ClientRole> clientRoles() {
        return clients().map(Client::getClientRoles).flatMap(List::stream);
    }

    public Stream<User> users() {
        return getRealms().stream().map(Realm::getUsers).flatMap(List::stream);
    }

    public Stream<Credential> credentials() {
        return users().map(User::getCredentials).flatMap(List::stream);
    }

    public Stream<RoleMappings<User>> userRealmRoleMappings() {
        return users().map(User::getRealmRoleMappings);
    }

    public Stream<ClientRoleMappings<User>> userClientRoleMappings() {
        return users().map(User::getClientRoleMappingsList).flatMap(List::stream);
    }

    public Stream<Group> groups() {
        return getRealms().stream().map(Realm::getGroups).flatMap(List::stream);
    }

    public Stream<ResourceServer> resourceServers() {
        return clients().filter(c -> c.getRepresentation().getAuthorizationServicesEnabled())
                .map(c -> c.getResourceServer());
    }

    public Stream<Scope> scopes() {
        return resourceServers().map(rs -> rs.getScopes()).flatMap(List::stream);
    }

    public Stream<Resource> resources() {
        return resourceServers().map(rs -> rs.getResources()).flatMap(List::stream);
    }

    public Stream<RolePolicy> rolePolicies() {
        return resourceServers().map(rs -> rs.getRolePolicies()).flatMap(List::stream);
    }

    public Stream<JsPolicy> jsPolicies() {
        return resourceServers().map(rs -> rs.getJsPolicies()).flatMap(List::stream);
    }

    public Stream<UserPolicy> userPolicies() {
        return resourceServers().map(rs -> rs.getUserPolicies()).flatMap(List::stream);
    }

    public Stream<ClientPolicy> clientPolicies() {
        return resourceServers().map(rs -> rs.getClientPolicies()).flatMap(List::stream);
    }

    public Stream<ResourcePermission> resourcePermissions() {
        return resourceServers().map(rs -> rs.getResourcePermissions()).flatMap(List::stream);
    }

    public Stream<ScopePermission> scopePermissions() {
        return resourceServers().map(rs -> rs.getScopePermissions()).flatMap(List::stream);
    }

}
