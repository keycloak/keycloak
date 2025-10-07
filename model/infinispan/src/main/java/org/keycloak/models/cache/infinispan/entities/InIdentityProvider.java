package org.keycloak.models.cache.infinispan.entities;


/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface InIdentityProvider extends Revisioned {
    boolean contains(String providerId);
}
