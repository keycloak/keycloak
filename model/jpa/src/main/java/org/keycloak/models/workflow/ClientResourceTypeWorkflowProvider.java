package org.keycloak.models.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.ClientEntity;
import org.keycloak.models.workflow.expression.BooleanConditionParser;
import org.keycloak.models.workflow.expression.EvaluatorUtils;
import org.keycloak.models.workflow.expression.PredicateEvaluator;
import org.keycloak.representations.workflows.WorkflowConstants;
import org.keycloak.utils.StringUtil;

import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_CONDITIONS;

public class ClientResourceTypeWorkflowProvider implements ResourceTypeSelector {

    private final EntityManager em;
    private final KeycloakSession session;

    public ClientResourceTypeWorkflowProvider(KeycloakSession session) {
        this.session = session;
        this.em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    @Override
    public List<String> getResourceIds(Workflow workflow) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<ClientEntity> userRoot = query.from(ClientEntity.class);
        List<Predicate> predicates = new ArrayList<>();

        // Subquery will find if a state record exists for the user and workflow
        // SELECT 1 FROM WorkflowActionStateEntity s WHERE s.resourceId = userRoot.id AND s.workflowId = :workflowId
        Subquery<Integer> subquery = query.subquery(Integer.class);
        Root<WorkflowStateEntity> stateRoot = subquery.from(WorkflowStateEntity.class);
        subquery.select(cb.literal(1));
        subquery.where(
                cb.and(
                        cb.equal(stateRoot.get("resourceId"), userRoot.get("id")),
                        cb.equal(stateRoot.get("workflowId"), workflow.getId())
                )
        );
        RealmModel realm = session.getContext().getRealm();
        predicates.add(cb.equal(userRoot.get("realmId"), realm.getId()));
        Predicate notExistsPredicate = cb.not(cb.exists(subquery));
        predicates.add(notExistsPredicate);

        predicates.add(getConditionsPredicate(workflow, cb, query, userRoot));

        query.select(userRoot.get("id")).where(predicates);

        int batchSize = Integer.parseInt(workflow.getConfig().getFirstOrDefault(WorkflowConstants.CONFIG_SCHEDULE_BATCH_SIZE, "100"));

        return em.createQuery(query).setMaxResults(batchSize).getResultList();
    }

    @Override
    public Object resolveResource(String resourceId) {
        Objects.requireNonNull(resourceId, "resourceId");
        return ResourceType.USERS.resolveResource(session, resourceId);
    }

    private Predicate getConditionsPredicate(Workflow workflow, CriteriaBuilder cb, CriteriaQuery<String> query, Root<ClientEntity> path) {
        MultivaluedHashMap<String, String> config = workflow.getConfig();
        String conditions = config.getFirst(CONFIG_CONDITIONS);

        if (StringUtil.isBlank(conditions)) {
            return cb.conjunction();
        }

        BooleanConditionParser.EvaluatorContext context = EvaluatorUtils.createEvaluatorContext(conditions);
        PredicateEvaluator evaluator = new PredicateEvaluator(session, cb, query, path);
        return evaluator.visit(context);
    }
}
