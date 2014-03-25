package org.keycloak.representations.idm;

import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthenticationMappingRepresentation {

    protected String self; // link
    protected String username;
    protected List<AuthenticationLinkRepresentation> authenticationLinks;

    public String getSelf() {
        return self;
    }

    public void setSelf(String self) {
        this.self = self;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<AuthenticationLinkRepresentation> getAuthenticationLinks() {
        return authenticationLinks;
    }

    public void setAuthenticationLinks(List<AuthenticationLinkRepresentation> authenticationLinks) {
        this.authenticationLinks = authenticationLinks;
    }


}
