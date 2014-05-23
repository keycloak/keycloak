package org.keycloak.models.entities;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OAuthClientEntity extends ClientEntity {
    protected boolean directGrantsOnly;

    public boolean isDirectGrantsOnly() {
        return directGrantsOnly;
    }

    public void setDirectGrantsOnly(boolean directGrantsOnly) {
        this.directGrantsOnly = directGrantsOnly;
    }
}
