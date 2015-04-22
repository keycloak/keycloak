package org.keycloak.representations.idm;

import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserConsentRepresentation {

    protected List<String> grantedRoles;           // points to roleIds
    protected List<String> grantedProtocolMappers; // points to protocolMapperIds

    public List<String> getGrantedRoles() {
        return grantedRoles;
    }

    public void setGrantedRoles(List<String> grantedRoles) {
        this.grantedRoles = grantedRoles;
    }

    public List<String> getGrantedProtocolMappers() {
        return grantedProtocolMappers;
    }

    public void setGrantedProtocolMappers(List<String> grantedProtocolMappers) {
        this.grantedProtocolMappers = grantedProtocolMappers;
    }
}
