package org.keycloak.models.jpa.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="findOAuthClientByName", query="select o from OAuthClientEntity o where o.name=:name and o.realm = :realm"),
        @NamedQuery(name="findOAuthClientByRealm", query="select o from OAuthClientEntity o where o.realm = :realm")

})
@Entity
public class OAuthClientEntity extends ClientEntity {
    @Column(name="DIRECT_GRANTS_ONLY")
    protected boolean directGrantsOnly;

    public boolean isDirectGrantsOnly() {
        return directGrantsOnly;
    }

    public void setDirectGrantsOnly(boolean directGrantsOnly) {
        this.directGrantsOnly = directGrantsOnly;
    }
}
