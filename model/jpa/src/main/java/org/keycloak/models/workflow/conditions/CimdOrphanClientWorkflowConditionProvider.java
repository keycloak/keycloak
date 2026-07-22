package org.keycloak.models.workflow.conditions;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.ClientAttributeEntity;
import org.keycloak.models.workflow.ResourceType;
import org.keycloak.models.workflow.WorkflowConditionProvider;
import org.keycloak.models.workflow.WorkflowExecutionContext;
import org.keycloak.models.workflow.WorkflowInvalidStateException;
import org.keycloak.utils.StringUtil;

/**
 * A workflow condition provider that identifies orphaned CIMD-registered clients.
 * <p>
 * A CIMD client is considered orphaned when the time elapsed since its cache expiry
 * exceeds a configured threshold. Specifically, when:
 * {@code currentTime - CIMD_CACHE_EXPIRY_TIME_IN_SEC > threshold}
 * <p>
 * The threshold (in seconds) is provided as the configuration parameter.
 */
public class CimdOrphanClientWorkflowConditionProvider implements WorkflowConditionProvider {

    /**
     * Client attribute name used by CIMD to store the cache expiry time (in epoch seconds).
     */
    public static final String CIMD_CACHE_EXPIRY_TIME_IN_SEC = "cimd.cache.expiry.time.in.sec";

    private final KeycloakSession session;
    private final String thresholdInSeconds;

    public CimdOrphanClientWorkflowConditionProvider(KeycloakSession session, String thresholdInSeconds) {
        this.session = session;
        this.thresholdInSeconds = thresholdInSeconds;
    }

    @Override
    public ResourceType getSupportedResourceType() {
        return ResourceType.CLIENTS;
    }

    @Override
    public boolean evaluate(WorkflowExecutionContext context) {
        validate();

        RealmModel realm = session.getContext().getRealm();
        ClientModel client = session.clients().getClientById(realm, context.getResourceId());

        if (client == null) {
            return false;
        }

        String cacheExpiryValue = client.getAttribute(CIMD_CACHE_EXPIRY_TIME_IN_SEC);
        if (cacheExpiryValue == null) {
            return false;
        }

        int cacheExpiryTime;
        try {
            cacheExpiryTime = Integer.parseInt(cacheExpiryValue);
        } catch (NumberFormatException e) {
            return false;
        }

        int threshold = Integer.parseInt(thresholdInSeconds);
        int currentTime = Time.currentTime();

        return currentTime - cacheExpiryTime > threshold;
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, CriteriaQuery<String> query, Root<?> resourceRoot) {
        validate();

        int threshold = Integer.parseInt(thresholdInSeconds);
        int currentTime = Time.currentTime();
        // A client is orphaned if: currentTime - cacheExpiryTime > threshold
        // which means: cacheExpiryTime < currentTime - threshold
        int maxExpiryTime = currentTime - threshold;
        String maxExpiryTimeStr = String.valueOf(maxExpiryTime);

        Subquery<String> subquery = query.subquery(String.class);
        Root<ClientAttributeEntity> attrRoot = subquery.from(ClientAttributeEntity.class);

        subquery.select(attrRoot.get("value"));
        subquery.where(
                cb.and(
                        cb.equal(attrRoot.get("client").get("id"), resourceRoot.get("id")),
                        cb.equal(attrRoot.get("name"), CIMD_CACHE_EXPIRY_TIME_IN_SEC),
                        cb.lessThan(attrRoot.get("value"), maxExpiryTimeStr)
                )
        );

        return cb.exists(subquery);
    }

    @Override
    public void validate() throws WorkflowInvalidStateException {
        if (StringUtil.isBlank(thresholdInSeconds)) {
            throw new WorkflowInvalidStateException("Threshold (in seconds) is not set.");
        }
        try {
            int threshold = Integer.parseInt(thresholdInSeconds);
            if (threshold < 0) {
                throw new WorkflowInvalidStateException("Threshold must be a non-negative integer.");
            }
        } catch (NumberFormatException e) {
            throw new WorkflowInvalidStateException("Threshold must be a valid integer representing seconds.");
        }
    }

    @Override
    public void close() {
    }
}
