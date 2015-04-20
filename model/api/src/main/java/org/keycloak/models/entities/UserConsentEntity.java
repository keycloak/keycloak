package org.keycloak.models.entities;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserConsentEntity extends AbstractIdentifiableEntity {

    private String userId;
    private String clientId;
    private List<String> grantedRoles = new ArrayList<String>();
    private List<String> grantedProtocolMappers = new ArrayList<String>();

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

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
