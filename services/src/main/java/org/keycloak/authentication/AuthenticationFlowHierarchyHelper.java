package org.keycloak.authentication;

import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.RealmModel;

import org.jboss.logging.Logger;

/**
 * Utility class for analyzing authentication flow hierarchies.
 *
 * <p>This class provides methods to navigate and analyze complex nested
 * authentication flow structures, including:</p>
 * <ul>
 *   <li>Finding parent flows of subflows</li>
 *   <li>Detecting executions after a subflow at parent level</li>
 *   <li>Recursive traversal of nested flow hierarchies</li>
 * </ul>
 *
 * <p>Used primarily for browser flow continuation after IDP redirect,
 * where we need to determine if additional authenticators follow
 * the IDP authentication.</p>
 *
 * <h3>Example Flow Structure:</h3>
 * <pre>
 * BrowserFlow (FLOW)
 *   ├─ Cookie (ALTERNATIVE)
 *   ├─ AuthSubflow (ALTERNATIVE)
 *   │   ├─ ConditionalSubflow (CONDITIONAL)
 *   │   │   ├─ Condition (condition)
 *   │   │   └─ IdpOtpSubflow (REQUIRED)
 *   │   │       ├─ IDP Redirector (REQUIRED)
 *   │   │       └─ OTP Form (REQUIRED)  ← Current subflow
 *   │   └─ Username/Password (ALTERNATIVE)  ← Found recursively!
 *   └─ Browser Forms (ALTERNATIVE)
 * </pre>
 *
 * @author Keycloak Team
 */
public class AuthenticationFlowHierarchyHelper {

    private static final Logger logger = Logger.getLogger(AuthenticationFlowHierarchyHelper.class);

    private final RealmModel realm;

    /**
     * Creates a new helper for the given realm.
     *
     * @param realm The realm containing the authentication flows
     */
    public AuthenticationFlowHierarchyHelper(RealmModel realm) {
        this.realm = realm;
    }

    /**
     * Checks if there are more executions after a subflow at parent level.
     *
     * <p>This method recursively traverses the flow hierarchy to determine
     * if any authenticators follow the given subflow, either:</p>
     * <ol>
     *   <li>Directly in the parent flow after the subflow</li>
     *   <li>In any ancestor flow after the parent flow</li>
     * </ol>
     *
     * <h3>Example Flow Structure:</h3>
     * <pre>
     * TopFlow (FLOW)
     *   ├─ Cookie (ALTERNATIVE)
     *   ├─ ParentSubflow (ALTERNATIVE)
     *   │   ├─ IDP Redirector (REQUIRED)
     *   │   └─ OTP Form (REQUIRED)      ← subFlowId
     *   └─ Username/Password (ALTERNATIVE) ← Returns TRUE (found!)
     * </pre>
     *
     * <h3>Algorithm:</h3>
     * <ol>
     *   <li>Find the direct parent flow of the subflow</li>
     *   <li>Check if there are executions after the subflow in the parent</li>
     *   <li>If not, recursively check after the parent flow itself</li>
     *   <li>Continue until top flow is reached or executions are found</li>
     * </ol>
     *
     * @param topFlow The top-level flow (usually browser flow)
     * @param subFlowId The ID of the subflow to check after
     * @return {@code true} if there are more executions after the subflow,
     *         {@code false} otherwise
     */
    public boolean hasMoreExecutionsAfterParentFlow(AuthenticationFlowModel topFlow, String subFlowId) {
        // If the subflow is the top flow, there are no parent-level executions
        if (topFlow.getId().equals(subFlowId)) {
            return false;
        }

        // Find the direct parent flow of the subflow
        FlowContext parentContext = findParentFlowContext(topFlow, subFlowId);
        if (parentContext == null) {
            logger.debugf("Could not find parent flow for subFlowId=%s", subFlowId);
            return false;
        }

        logger.debugf("Found parent flow '%s' for subFlow '%s'",
            parentContext.getParentFlow().getAlias(), subFlowId);

        // Check if there are more executions in the parent flow after the subflow
        boolean hasMoreInParent = hasMoreExecutionsInFlowAfterSubFlow(
            parentContext.getParentFlow().getId(), subFlowId);

        if (hasMoreInParent) {
            logger.debugf("Found more executions in parent flow '%s' after subFlow",
                parentContext.getParentFlow().getAlias());
            return true;
        }

        // If not, check recursively if there are more executions after the parent flow
        // (only if the parent is not the top flow)
        if (!parentContext.getParentFlow().getId().equals(topFlow.getId())) {
            logger.debugf("No more executions in parent flow, checking recursively after parent flow '%s'",
                parentContext.getParentFlow().getAlias());
            return hasMoreExecutionsAfterParentFlow(topFlow, parentContext.getParentFlow().getId());
        }

        return false;
    }

    /**
     * Finds the direct parent flow context of a subflow.
     *
     * <p>This method recursively searches through the flow hierarchy to find
     * the parent flow that directly contains the target subflow.</p>
     *
     * <h3>Example:</h3>
     * <pre>
     * TopFlow
     *   └─ ParentFlow           ← This will be returned
     *       └─ TargetSubflow    ← Looking for this
     * </pre>
     *
     * @param currentFlow The flow to start searching from
     * @param targetSubFlowId The ID of the subflow to find the parent for
     * @return {@link FlowContext} containing parent flow and execution,
     *         or {@code null} if not found
     */
    public FlowContext findParentFlowContext(AuthenticationFlowModel currentFlow, String targetSubFlowId) {
        List<AuthenticationExecutionModel> executions =
            realm.getAuthenticationExecutionsStream(currentFlow.getId()).collect(Collectors.toList());

        for (AuthenticationExecutionModel execution : executions) {
            if (execution.isAuthenticatorFlow()) {
                // Direct match found
                if (execution.getFlowId().equals(targetSubFlowId)) {
                    return new FlowContext(currentFlow, execution);
                }

                // Search recursively in this subflow
                AuthenticationFlowModel subFlow = realm.getAuthenticationFlowById(execution.getFlowId());
                if (subFlow != null) {
                    FlowContext found = findParentFlowContext(subFlow, targetSubFlowId);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Checks if there are more executions in a flow after a specific subflow.
     *
     * <p>This method checks only the direct children of the given flow,
     * not recursively in nested subflows.</p>
     *
     * @param flowId The ID of the flow to check
     * @param subFlowId The ID of the subflow
     * @return {@code true} if there are executions after the subflow in this flow,
     *         {@code false} otherwise
     */
    private boolean hasMoreExecutionsInFlowAfterSubFlow(String flowId, String subFlowId) {
        List<AuthenticationExecutionModel> executions =
            realm.getAuthenticationExecutionsStream(flowId).collect(Collectors.toList());

        boolean foundSubFlow = false;
        for (AuthenticationExecutionModel execution : executions) {
            if (foundSubFlow) {
                // Check if execution is active (not disabled) and is an authenticator
                if (!execution.isDisabled() &&
                    (execution.isAuthenticatorFlow() || execution.getAuthenticator() != null)) {
                    return true;
                }
            }
            if (execution.isAuthenticatorFlow() && execution.getFlowId().equals(subFlowId)) {
                foundSubFlow = true;
            }
        }
        return false;
    }

    /**
     * Context class holding parent flow and the execution that references the subflow.
     *
     * <p>This class provides access to both the parent flow and the execution
     * model that represents the subflow reference within the parent.</p>
     */
    public static class FlowContext {
        private final AuthenticationFlowModel parentFlow;
        private final AuthenticationExecutionModel subFlowExecution;

        /**
         * Creates a new flow context.
         *
         * @param parentFlow The parent flow containing the subflow
         * @param subFlowExecution The execution in the parent that references the subflow
         */
        public FlowContext(AuthenticationFlowModel parentFlow,
                          AuthenticationExecutionModel subFlowExecution) {
            this.parentFlow = parentFlow;
            this.subFlowExecution = subFlowExecution;
        }

        /**
         * Gets the parent flow.
         *
         * @return The parent flow model
         */
        public AuthenticationFlowModel getParentFlow() {
            return parentFlow;
        }

        /**
         * Gets the subflow execution.
         *
         * @return The execution model representing the subflow in the parent
         */
        public AuthenticationExecutionModel getSubFlowExecution() {
            return subFlowExecution;
        }
    }
}
