package org.keycloak.models.cache.infinispan.entities;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface Revisioned {
    String getId();
    Long getRevision();
    void setRevision(Long revision);
}
