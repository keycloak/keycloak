package org.keycloak.performance.dataset.idm;

import java.util.Iterator;
import java.util.List;
import javax.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.performance.dataset.Creatable;
import org.keycloak.performance.iteration.FilteredIterator;
import org.keycloak.performance.iteration.RandomIterator;

/**
 *
 * @author tkyjovsk
 */
public class User extends RoleMapper<UserRepresentation> implements Creatable<UserRepresentation> {

    private List<Credential> credentials;
    private RoleMappings<User> realmRoleMappings;
    private List<ClientRoleMappings<User>> clientRoleMappingsList;

    public User(Realm realm, int index) {
        super(realm, index);
    }

    @Override
    public UserRepresentation newRepresentation() {
        return new UserRepresentation();
    }

    @Override
    public String toString() {
        return getRepresentation().getUsername();
    }

    public void setRealmRoleMappings(RoleMappings<User> realmRoleMappings) {
        this.realmRoleMappings = realmRoleMappings;
    }

    public void setClientRoleMappingsList(List<ClientRoleMappings<User>> clientRoleMappingsList) {
        this.clientRoleMappingsList = clientRoleMappingsList;
    }

    public RoleMappings<User> getRealmRoleMappings() {
        return realmRoleMappings;
    }

    public List<ClientRoleMappings<User>> getClientRoleMappingsList() {
        return clientRoleMappingsList;
    }

    public List<Credential> getCredentials() {
        return credentials;
    }

    public void setCredentials(List<Credential> credentials) {
        this.credentials = credentials;
    }

    public UserResource resource(Keycloak adminClient) {
        return getRealm().resource(adminClient).users().get(getIdAndReadIfNull(adminClient));
    }

    @Override
    public UserRepresentation read(Keycloak adminClient) {
        return getRealm().resource(adminClient).users().search(getRepresentation().getUsername()).get(0);
    }

    @Override
    public Response create(Keycloak adminClient) {
        return getRealm().resource(adminClient).users().create(getRepresentation());
    }

    @Override
    public void update(Keycloak adminClient) {
        resource(adminClient).update(getRepresentation());
    }

    @Override
    public void delete(Keycloak adminClient) {
        resource(adminClient).remove();
    }

    @Override
    public RoleMappingResource roleMappingResource(Keycloak adminClient) {
        return resource(adminClient).roles();
    }

    public Iterator<Client> randomClientIterator() {
        return new RandomIterator<>(getRealm().getClients());
    }

    public Iterator<Client> randomConfidentialClientIterator() {
        return new FilteredIterator<>(new RandomIterator<>(getRealm().getClients()),
                c -> !c.getRepresentation().isPublicClient() && !c.getRepresentation().isBearerOnly()
        );
    }

}
