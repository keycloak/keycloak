package org.keycloak.representations.idm;

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmAuditRepresentation {
    protected boolean auditEnabled;
    protected Long auditExpiration;
    protected List<String> auditListeners;

    public boolean isAuditEnabled() {
        return auditEnabled;
    }

    public void setAuditEnabled(boolean auditEnabled) {
        this.auditEnabled = auditEnabled;
    }

    public Long getAuditExpiration() {
        return auditExpiration;
    }

    public void setAuditExpiration(Long auditExpiration) {
        this.auditExpiration = auditExpiration;
    }

    public List<String> getAuditListeners() {
        return auditListeners;
    }

    public void setAuditListeners(List<String> auditListeners) {
        this.auditListeners = auditListeners;
    }
}
