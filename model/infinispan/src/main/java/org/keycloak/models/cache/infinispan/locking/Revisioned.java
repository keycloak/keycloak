package org.keycloak.models.cache.infinispan.locking;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface Revisioned {
    Long getRevision();
    void setRevision(Long revision);
}
