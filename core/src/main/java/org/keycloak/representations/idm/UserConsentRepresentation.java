package org.keycloak.representations.idm;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserConsentRepresentation {

    protected String clientId;

    // Key is protocol, Value is list of granted consents for this protocol
    protected Map<String, List<String>> grantedProtocolMappers;

    protected List<String> grantedRealmRoles;

    // Key is clientId, Value is list of granted roles of this client
    protected Map<String, List<String>> grantedClientRoles;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Map<String, List<String>> getGrantedProtocolMappers() {
        return grantedProtocolMappers;
    }

    public void setGrantedProtocolMappers(Map<String, List<String>> grantedProtocolMappers) {
        this.grantedProtocolMappers = grantedProtocolMappers;
    }

    public List<String> getGrantedRealmRoles() {
        return grantedRealmRoles;
    }

    public void setGrantedRealmRoles(List<String> grantedRealmRoles) {
        this.grantedRealmRoles = grantedRealmRoles;
    }

    public Map<String, List<String>> getGrantedClientRoles() {
        return grantedClientRoles;
    }

    public void setGrantedClientRoles(Map<String, List<String>> grantedClientRoles) {
        this.grantedClientRoles = grantedClientRoles;
    }
}
