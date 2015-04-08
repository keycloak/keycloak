package org.keycloak.federation.ldap.idm.model;

import java.util.Date;

/**
 * Abstract base class for IdentityType implementations
 *
 * @author Shane Bryzak
 */
public abstract class AbstractIdentityType extends AbstractAttributedType implements IdentityType {

    private static final long serialVersionUID = 2843998332737143820L;

    private boolean enabled = true;
    private Date createdDate = new Date();
    private Date expirationDate = null;

    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    @AttributeProperty
    public Date getExpirationDate() {
        return this.expirationDate;
    }

    @Override
    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    @Override
    @AttributeProperty
    public Date getCreatedDate() {
        return this.createdDate;
    }

    @Override
    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!getClass().isInstance(obj)) {
            return false;
        }

        IdentityType other = (IdentityType) obj;

        return (getId() != null && other.getId() != null)
                && (getId().equals(other.getId()));
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}

