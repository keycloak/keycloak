package org.keycloak.account.freemarker.model;

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
public class ConsentBean {

    private List<ClientGrantBean> clientGrants = new LinkedList<ClientGrantBean>();

    public ConsentBean(UserModel user) {
        List<UserConsentModel> grantedConsents = user.getConsents();
        for (UserConsentModel consent : grantedConsents) {
            ClientModel client = consent.getClient();

            List<RoleModel> realmRolesGranted = new LinkedList<RoleModel>();
            MultivaluedHashMap<String, ClientRoleEntry> resourceRolesGranted = new MultivaluedHashMap<String, ClientRoleEntry>();
            for (RoleModel role : consent.getGrantedRoles()) {
                if (role.getContainer() instanceof RealmModel) {
                    realmRolesGranted.add(role);
                } else {
                    ClientModel currentClient = (ClientModel) role.getContainer();
                    ClientRoleEntry clientRole = new ClientRoleEntry(currentClient.getClientId(), currentClient.getName(),
                            role.getName(), role.getDescription());
                    resourceRolesGranted.add(currentClient.getClientId(), clientRole);
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
        private final MultivaluedHashMap<String, ClientRoleEntry> resourceRolesGranted;
        private final ClientModel client;
        private final List<String> claimsGranted;

        public ClientGrantBean(List<RoleModel> realmRolesGranted, MultivaluedHashMap<String, ClientRoleEntry> resourceRolesGranted,
                               ClientModel client, List<String> claimsGranted) {
            this.realmRolesGranted = realmRolesGranted;
            this.resourceRolesGranted = resourceRolesGranted;
            this.client = client;
            this.claimsGranted = claimsGranted;
        }

        public List<RoleModel> getRealmRolesGranted() {
            return realmRolesGranted;
        }

        public MultivaluedHashMap<String, ClientRoleEntry> getResourceRolesGranted() {
            return resourceRolesGranted;
        }

        public ClientModel getClient() {
            return client;
        }

        public List<String> getClaimsGranted() {
            return claimsGranted;
        }

    }

    // Same class used in OAuthGrantBean as well. Maybe should be merged into common-freemarker...
    public static class ClientRoleEntry {

        private final String clientId;
        private final String clientName;
        private final String roleName;
        private final String roleDescription;

        public ClientRoleEntry(String clientId, String clientName, String roleName, String roleDescription) {
            this.clientId = clientId;
            this.clientName = clientName;
            this.roleName = roleName;
            this.roleDescription = roleDescription;
        }

        public String getClientId() {
            return clientId;
        }

        public String getClientName() {
            return clientName;
        }

        public String getRoleName() {
            return roleName;
        }

        public String getRoleDescription() {
            return roleDescription;
        }
    }
}
