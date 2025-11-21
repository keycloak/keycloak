package org.keycloak.models.workflow.conditions;

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
import org.keycloak.models.workflow.WorkflowConditionProvider;
import org.keycloak.models.workflow.WorkflowExecutionContext;
import org.keycloak.models.workflow.WorkflowInvalidStateException;
import org.keycloak.utils.StringUtil;

public class IdentityProviderWorkflowConditionProvider implements WorkflowConditionProvider {

    private final String expectedAlias;
    private final KeycloakSession session;

    public IdentityProviderWorkflowConditionProvider(KeycloakSession session, String expectedAlias) {
        this.session = session;
        this.expectedAlias = expectedAlias;
    }

    @Override
    public boolean evaluate(WorkflowExecutionContext context) {
        validate();

        RealmModel realm = session.getContext().getRealm();
        UserModel user = session.users().getUserById(realm, context.getResourceId());
        if (user == null) {
            return false;
        }

        Stream<FederatedIdentityModel> federatedIdentities = session.users().getFederatedIdentitiesStream(realm, user);
        return federatedIdentities
                .map(FederatedIdentityModel::getIdentityProvider)
                .anyMatch(expectedAlias::equals);
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, CriteriaQuery<String> query, Root<?> path) {
        Subquery<Integer> subquery = query.subquery(Integer.class);
        Root<FederatedIdentityEntity> from = subquery.from(FederatedIdentityEntity.class);

        subquery.select(cb.literal(1));
        subquery.where(
                cb.and(
                        cb.equal(from.get("user").get("id"), path.get("id")),
                        cb.equal(from.get("identityProvider"), expectedAlias)
                )
        );

        return cb.exists(subquery);
    }

    @Override
    public void validate() {
        if (StringUtil.isBlank(expectedAlias)) {
            throw new WorkflowInvalidStateException("Expected identity provider alias is not set.");
        }
        if (session.identityProviders().getByAlias(expectedAlias) == null) {
            throw new WorkflowInvalidStateException(String.format("Identity provider %s does not exist.", expectedAlias));
        }
    }

    @Override
    public void close() {

    }
}
