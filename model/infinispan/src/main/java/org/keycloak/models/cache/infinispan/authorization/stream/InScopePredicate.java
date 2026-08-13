package org.keycloak.models.cache.infinispan.authorization.stream;

import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.cache.infinispan.authorization.entities.InScope;
import org.keycloak.models.cache.infinispan.entities.Revisioned;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@ProtoTypeId(Marshalling.IN_SCOPE_PREDICATE)
public class InScopePredicate implements Predicate<Map.Entry<String, Revisioned>> {
    private final String scopeId;

    private InScopePredicate(String scopeId) {
        this.scopeId = Objects.requireNonNull(scopeId);
    }

    @ProtoFactory
    public static InScopePredicate create(String scopeId) {
        return new InScopePredicate(scopeId);
    }

    @ProtoField(1)
    String getScopeId() {
        return scopeId;
    }

    @Override
    public boolean test(Map.Entry<String, Revisioned> entry) {
        return entry.getValue() instanceof InScope inScope && scopeId.equals(inScope.getScopeId());
    }

}
