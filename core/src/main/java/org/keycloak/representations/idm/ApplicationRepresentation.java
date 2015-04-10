package org.keycloak.representations.idm;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Deprecated
public class ApplicationRepresentation extends ClientRepresentation {
    protected String name;
    @Deprecated
    protected ClaimRepresentation claims;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ClaimRepresentation getClaims() {
        return claims;
    }

    public void setClaims(ClaimRepresentation claims) {
        this.claims = claims;
    }
}
