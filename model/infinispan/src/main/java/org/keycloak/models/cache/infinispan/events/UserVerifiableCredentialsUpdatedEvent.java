
package org.keycloak.models.cache.infinispan.events;

import java.util.Set;

import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.cache.infinispan.UserCacheManager;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * Invalidation event for user verifiable credentials cache entries
 */
@ProtoTypeId(Marshalling.USER_VERIFIABLE_CREDENTIALS_UPDATED_EVENT)
public class UserVerifiableCredentialsUpdatedEvent extends InvalidationEvent implements UserCacheInvalidationEvent {

    private UserVerifiableCredentialsUpdatedEvent(String id) {
        super(id);
    }

    @ProtoFactory
    public static UserVerifiableCredentialsUpdatedEvent create(String id) {
        return new UserVerifiableCredentialsUpdatedEvent(id);
    }

    @Override
    public String toString() {
        return String.format("UserVerifiableCredentialsUpdatedEvent [ userId=%s ]", getId());
    }

    @Override
    public void addInvalidations(UserCacheManager userCache, Set<String> invalidations) {
        userCache.verifiableCredentialsInvalidation(getId(), invalidations);
    }

}
