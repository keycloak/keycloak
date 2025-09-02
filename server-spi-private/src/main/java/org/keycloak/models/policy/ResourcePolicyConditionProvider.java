package org.keycloak.models.policy;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.keycloak.provider.Provider;

public interface ResourcePolicyConditionProvider extends Provider {

    boolean evaluate(ResourcePolicyEvent event);

    default Predicate toPredicate(CriteriaBuilder cb, CriteriaQuery<String> query, Root<?> userRoot) {
        return null;
    }
}
