package org.keycloak.account.freemarker.model;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import org.keycloak.models.ClientModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.util.MultivaluedHashMap;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AccessBean {

    private List<ClientGrantBean> clientGrants = new LinkedList<ClientGrantBean>();

    public AccessBean(RealmModel realm, UserModel user, URI baseUri, String stateChecker) {
        List<UserConsentModel> grantedConsents = user.getGrantedConsents();
        for (UserConsentModel consent : grantedConsents) {
            ClientModel client = consent.getClient();

            List<RoleModel> realmRolesGranted = new LinkedList<RoleModel>();
            MultivaluedHashMap<String, RoleModel> resourceRolesGranted = new MultivaluedHashMap<String, RoleModel>();
            for (RoleModel role : consent.getGrantedRoles()) {
                if (role.getContainer() instanceof RealmModel) {
                    realmRolesGranted.add(role);
                } else {
                    resourceRolesGranted.add(((ClientModel) role.getContainer()).getClientId(), role);
                }
            }

            List<String> claimsGranted = new LinkedList<String>();
            for (ProtocolMapperModel protocolMapper : consent.getGrantedProtocolMappers()) {
                claimsGranted.add(protocolMapper.getConsentText());
            }

            ClientGrantBean clientGrant = new ClientGrantBean(realmRolesGranted, resourceRolesGranted, client, claimsGranted);
            clientGrants.add(clientGrant);
        }
    }

    public List<ClientGrantBean> getClientGrants() {
        return clientGrants;
    }

    public static class ClientGrantBean {

        private final List<RoleModel> realmRolesGranted;
        private final MultivaluedHashMap<String, RoleModel> resourceRolesGranted;
        private final ClientModel client;
        private final List<String> claimsGranted;

        public ClientGrantBean(List<RoleModel> realmRolesGranted, MultivaluedHashMap<String, RoleModel> resourceRolesGranted,
                               ClientModel client, List<String> claimsGranted) {
            this.realmRolesGranted = realmRolesGranted;
            this.resourceRolesGranted = resourceRolesGranted;
            this.client = client;
            this.claimsGranted = claimsGranted;
        }

        public List<RoleModel> getRealmRolesGranted() {
            return realmRolesGranted;
        }

        public MultivaluedHashMap<String, RoleModel> getResourceRolesGranted() {
            return resourceRolesGranted;
        }

        public ClientModel getClient() {
            return client;
        }

        public List<String> getClaimsGranted() {
            return claimsGranted;
        }

    }
}
