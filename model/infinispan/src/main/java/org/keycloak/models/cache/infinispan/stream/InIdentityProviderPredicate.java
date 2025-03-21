package org.keycloak.models.cache.infinispan.stream;

import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.keycloak.models.cache.infinispan.entities.InIdentityProvider;
import org.keycloak.models.cache.infinispan.entities.Revisioned;
import org.keycloak.marshalling.Marshalling;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@ProtoTypeId(Marshalling.IN_IDENTITY_PROVIDER_PREDICATE)
public class InIdentityProviderPredicate implements Predicate<Map.Entry<String, Revisioned>> {
    private String id;

    public static InIdentityProviderPredicate create() {
        return new InIdentityProviderPredicate();
    }

    public InIdentityProviderPredicate provider(String id) {
        this.id = id;
        return this;
    }

    @ProtoField(1)
    String getId() {
        return id;
    }

    void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean test(Map.Entry<String, Revisioned> entry) {
        return entry.getValue() instanceof InIdentityProvider provider && provider.contains(id);
    }

}
