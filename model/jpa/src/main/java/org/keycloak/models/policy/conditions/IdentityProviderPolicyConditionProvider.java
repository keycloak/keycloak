package org.keycloak.models.policy.conditions;

import java.util.List;
import java.util.stream.Stream;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.FederatedIdentityEntity;
import org.keycloak.models.policy.ResourcePolicyConditionProvider;
import org.keycloak.models.policy.ResourcePolicyEvent;
import org.keycloak.models.policy.ResourceType;

public class IdentityProviderPolicyConditionProvider implements ResourcePolicyConditionProvider {

    private final List<String> expectedAliases;
    private final KeycloakSession session;

    public IdentityProviderPolicyConditionProvider(KeycloakSession session, List<String> expectedAliases) {
        this.session = session;
        this.expectedAliases = expectedAliases;;
    }

    @Override
    public boolean evaluate(ResourcePolicyEvent event) {
        if (!ResourceType.USERS.equals(event.getResourceType())) {
            return false;
        }

        String userId = event.getResourceId();
        RealmModel realm = session.getContext().getRealm();
        UserModel user = session.users().getUserById(realm, userId);
        Stream<FederatedIdentityModel> federatedIdentities = session.users().getFederatedIdentitiesStream(realm, user);

        return federatedIdentities
                .map(FederatedIdentityModel::getIdentityProvider)
                .anyMatch(expectedAliases::contains);
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, CriteriaQuery<String> query, Root<?> path) {
        Subquery<Integer> subquery = query.subquery(Integer.class);
        Root<FederatedIdentityEntity> from = subquery.from(FederatedIdentityEntity.class);

        subquery.select(cb.literal(1));
        subquery.where(
                cb.and(
                        cb.equal(from.get("user").get("id"), path.get("id")),
                        from.get("identityProvider").in(expectedAliases)
                )
        );

        return cb.exists(subquery);
    }

    @Override
    public void close() {

    }
}
