package org.keycloak.authentication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.RealmModel;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link AuthenticationFlowHierarchyHelper}.
 *
 * <p>These tests verify the correct behavior of flow hierarchy analysis,
 * including flat flows, nested subflows, and deeply nested structures.</p>
 *
 * @author Keycloak Team
 */
public class AuthenticationFlowHierarchyHelperTest {

    private TestRealmModel realm;
    private AuthenticationFlowHierarchyHelper helper;

    @Before
    public void setup() {
        TestRealmModel testRealm = new TestRealmModel();
        realm = testRealm;
        helper = new AuthenticationFlowHierarchyHelper(testRealm.getProxy());
    }

    /**
     * Test: Flat flow with no executions after subflow
     *
     * <pre>
     * TopFlow
     *   ├─ Cookie (ALTERNATIVE)
     *   └─ SubFlow (ALTERNATIVE)  ← No executions after this
     * </pre>
     *
     * Expected: false (no more executions)
     */
    @Test
    public void testFlatFlow_NoMoreExecutions() {
        // Setup flow structure
        AuthenticationFlowModel topFlow = createFlow("top-flow", "TopFlow");
        AuthenticationFlowModel subFlow = createFlow("sub-flow", "SubFlow");

        realm.addFlow(topFlow);
        realm.addFlow(subFlow);

        realm.addExecution("top-flow", createAuthenticatorExecution("cookie", "top-flow"));
        realm.addExecution("top-flow", createFlowExecution("sub-flow", "top-flow"));

        // Test
        boolean result = helper.hasMoreExecutionsAfterParentFlow(topFlow, "sub-flow");

        // Verify
        assertFalse("Should return false when no executions after subflow", result);
    }

    /**
     * Test: Flat flow with executions after subflow
     *
     * <pre>
     * TopFlow
     *   ├─ Cookie (ALTERNATIVE)
     *   ├─ SubFlow (ALTERNATIVE)         ← Checking after this
     *   └─ Username/Password (ALTERNATIVE) ← Found!
     * </pre>
     *
     * Expected: true (execution found after subflow)
     */
    @Test
    public void testFlatFlow_WithExecutionsAfter() {
        // Setup flow structure
        AuthenticationFlowModel topFlow = createFlow("top-flow", "TopFlow");
        AuthenticationFlowModel subFlow = createFlow("sub-flow", "SubFlow");

        realm.addFlow(topFlow);
        realm.addFlow(subFlow);

        realm.addExecution("top-flow", createAuthenticatorExecution("cookie", "top-flow"));
        realm.addExecution("top-flow", createFlowExecution("sub-flow", "top-flow"));
        realm.addExecution("top-flow", createAuthenticatorExecution("username-password", "top-flow"));

        // Test
        boolean result = helper.hasMoreExecutionsAfterParentFlow(topFlow, "sub-flow");

        // Verify
        assertTrue("Should return true when execution found after subflow", result);
    }

    /**
     * Test: Single-level nested subflow with execution after parent
     *
     * <pre>
     * TopFlow
     *   ├─ Cookie (ALTERNATIVE)
     *   ├─ ParentSubflow (ALTERNATIVE)
     *   │   └─ TargetSubflow (REQUIRED)  ← Checking after this
     *   └─ Username/Password (ALTERNATIVE) ← Found at parent level!
     * </pre>
     *
     * Expected: true (execution found at parent level)
     */
    @Test
    public void testNestedFlow_OneLevel_WithExecutionAfterParent() {
        // Setup flow structure
        AuthenticationFlowModel topFlow = createFlow("top-flow", "TopFlow");
        AuthenticationFlowModel parentSubflow = createFlow("parent-subflow", "ParentSubflow");
        AuthenticationFlowModel targetSubflow = createFlow("target-subflow", "TargetSubflow");

        realm.addFlow(topFlow);
        realm.addFlow(parentSubflow);
        realm.addFlow(targetSubflow);

        realm.addExecution("top-flow", createAuthenticatorExecution("cookie", "top-flow"));
        realm.addExecution("top-flow", createFlowExecution("parent-subflow", "top-flow"));
        realm.addExecution("top-flow", createAuthenticatorExecution("username-password", "top-flow"));

        realm.addExecution("parent-subflow", createFlowExecution("target-subflow", "parent-subflow"));

        // Test
        boolean result = helper.hasMoreExecutionsAfterParentFlow(topFlow, "target-subflow");

        // Verify
        assertTrue("Should return true when execution found at parent level", result);
    }

    /**
     * Test: Two-level nested subflow with execution after grandparent
     *
     * <pre>
     * TopFlow
     *   ├─ Cookie (ALTERNATIVE)
     *   ├─ Level1Subflow (ALTERNATIVE)
     *   │   └─ Level2Subflow (REQUIRED)
     *   │       └─ TargetSubflow (REQUIRED)  ← Checking after this
     *   └─ Username/Password (ALTERNATIVE)    ← Found at grandparent level!
     * </pre>
     *
     * Expected: true (execution found at grandparent level through recursion)
     */
    @Test
    public void testNestedFlow_TwoLevels_WithExecutionAfterGrandparent() {
        // Setup flow structure
        AuthenticationFlowModel topFlow = createFlow("top-flow", "TopFlow");
        AuthenticationFlowModel level1Subflow = createFlow("level1-subflow", "Level1Subflow");
        AuthenticationFlowModel level2Subflow = createFlow("level2-subflow", "Level2Subflow");
        AuthenticationFlowModel targetSubflow = createFlow("target-subflow", "TargetSubflow");

        realm.addFlow(topFlow);
        realm.addFlow(level1Subflow);
        realm.addFlow(level2Subflow);
        realm.addFlow(targetSubflow);

        realm.addExecution("top-flow", createAuthenticatorExecution("cookie", "top-flow"));
        realm.addExecution("top-flow", createFlowExecution("level1-subflow", "top-flow"));
        realm.addExecution("top-flow", createAuthenticatorExecution("username-password", "top-flow"));

        realm.addExecution("level1-subflow", createFlowExecution("level2-subflow", "level1-subflow"));
        realm.addExecution("level2-subflow", createFlowExecution("target-subflow", "level2-subflow"));

        // Test
        boolean result = helper.hasMoreExecutionsAfterParentFlow(topFlow, "target-subflow");

        // Verify
        assertTrue("Should return true when execution found at grandparent level through recursion", result);
    }

    /**
     * Test: Three-level nested subflow (deep nesting)
     *
     * <pre>
     * TopFlow
     *   └─ L1 → L2 → L3 → TargetSubflow  ← No executions after any level
     * </pre>
     *
     * Expected: false (no executions at any level)
     */
    @Test
    public void testNestedFlow_ThreeLevels_NoExecutionsAfter() {
        // Setup flow structure
        AuthenticationFlowModel topFlow = createFlow("top-flow", "TopFlow");
        AuthenticationFlowModel l1 = createFlow("l1", "L1");
        AuthenticationFlowModel l2 = createFlow("l2", "L2");
        AuthenticationFlowModel l3 = createFlow("l3", "L3");
        AuthenticationFlowModel target = createFlow("target", "Target");

        realm.addFlow(topFlow);
        realm.addFlow(l1);
        realm.addFlow(l2);
        realm.addFlow(l3);
        realm.addFlow(target);

        realm.addExecution("top-flow", createFlowExecution("l1", "top-flow"));
        realm.addExecution("l1", createFlowExecution("l2", "l1"));
        realm.addExecution("l2", createFlowExecution("l3", "l2"));
        realm.addExecution("l3", createFlowExecution("target", "l3"));

        // Test
        boolean result = helper.hasMoreExecutionsAfterParentFlow(topFlow, "target");

        // Verify
        assertFalse("Should return false when no executions at any level", result);
    }

    /**
     * Test: Conditional flow with execution after
     *
     * <pre>
     * TopFlow
     *   ├─ AuthSubflow (ALTERNATIVE)
     *   │   └─ ConditionalSubflow (CONDITIONAL)
     *   │       ├─ Condition
     *   │       └─ IdpOtpSubflow (REQUIRED)  ← Checking after this
     *   └─ BrowserForms (ALTERNATIVE)         ← Found!
     * </pre>
     *
     * Expected: true (execution found after conditional flow)
     */
    @Test
    public void testConditionalFlow_WithExecutionsAfter() {
        // Setup flow structure
        AuthenticationFlowModel topFlow = createFlow("top-flow", "TopFlow");
        AuthenticationFlowModel authSubflow = createFlow("auth-subflow", "AuthSubflow");
        AuthenticationFlowModel conditionalSubflow = createFlow("conditional-subflow", "ConditionalSubflow");
        AuthenticationFlowModel idpOtpSubflow = createFlow("idp-otp-subflow", "IdpOtpSubflow");

        realm.addFlow(topFlow);
        realm.addFlow(authSubflow);
        realm.addFlow(conditionalSubflow);
        realm.addFlow(idpOtpSubflow);

        realm.addExecution("top-flow", createFlowExecution("auth-subflow", "top-flow"));
        realm.addExecution("top-flow", createAuthenticatorExecution("browser-forms", "top-flow"));

        realm.addExecution("auth-subflow", createFlowExecution("conditional-subflow", "auth-subflow"));

        realm.addExecution("conditional-subflow", createAuthenticatorExecution("condition", "conditional-subflow"));
        realm.addExecution("conditional-subflow", createFlowExecution("idp-otp-subflow", "conditional-subflow"));

        // Test
        boolean result = helper.hasMoreExecutionsAfterParentFlow(topFlow, "idp-otp-subflow");

        // Verify
        assertTrue("Should return true when execution found after conditional flow", result);
    }

    /**
     * Test: Edge case where subFlowId equals topFlowId
     *
     * Expected: false (no parent level for top flow)
     */
    @Test
    public void testSubFlowIsTopFlow_ReturnsFalse() {
        // Setup
        AuthenticationFlowModel topFlow = createFlow("top-flow", "TopFlow");
        realm.addFlow(topFlow);

        // Test
        boolean result = helper.hasMoreExecutionsAfterParentFlow(topFlow, "top-flow");

        // Verify
        assertFalse("Should return false when subFlowId equals topFlowId", result);
    }

    /**
     * Test: Edge case where parent cannot be found
     *
     * Expected: false (parent not found)
     */
    @Test
    public void testParentNotFound_ReturnsFalse() {
        // Setup
        AuthenticationFlowModel topFlow = createFlow("top-flow", "TopFlow");
        realm.addFlow(topFlow);
        realm.addExecution("top-flow", createAuthenticatorExecution("cookie", "top-flow"));

        // Test with non-existent subflow ID
        boolean result = helper.hasMoreExecutionsAfterParentFlow(topFlow, "non-existent-subflow");

        // Verify
        assertFalse("Should return false when parent flow cannot be found", result);
    }

    /**
     * Test: Finding parent context for direct child
     *
     * <pre>
     * TopFlow
     *   └─ ChildSubflow  ← Find parent of this
     * </pre>
     *
     * Expected: FlowContext with topFlow as parent
     */
    @Test
    public void testFindParentFlowContext_DirectChild() {
        // Setup
        AuthenticationFlowModel topFlow = createFlow("top-flow", "TopFlow");
        AuthenticationFlowModel childSubflow = createFlow("child-subflow", "ChildSubflow");

        realm.addFlow(topFlow);
        realm.addFlow(childSubflow);

        realm.addExecution("top-flow", createFlowExecution("child-subflow", "top-flow"));

        // Test
        AuthenticationFlowHierarchyHelper.FlowContext context =
            helper.findParentFlowContext(topFlow, "child-subflow");

        // Verify
        assertNotNull("Should find parent context", context);
        assertEquals("Parent should be topFlow", "top-flow", context.getParentFlow().getId());
        assertEquals("Execution should reference child subflow", "child-subflow",
            context.getSubFlowExecution().getFlowId());
    }

    /**
     * Test: Finding parent context for nested child (2 levels)
     *
     * <pre>
     * TopFlow
     *   └─ Level1
     *       └─ Level2  ← Find parent of this
     * </pre>
     *
     * Expected: FlowContext with level1 as parent
     */
    @Test
    public void testFindParentFlowContext_NestedChild() {
        // Setup
        AuthenticationFlowModel topFlow = createFlow("top-flow", "TopFlow");
        AuthenticationFlowModel level1 = createFlow("level1", "Level1");
        AuthenticationFlowModel level2 = createFlow("level2", "Level2");

        realm.addFlow(topFlow);
        realm.addFlow(level1);
        realm.addFlow(level2);

        realm.addExecution("top-flow", createFlowExecution("level1", "top-flow"));
        realm.addExecution("level1", createFlowExecution("level2", "level1"));

        // Test
        AuthenticationFlowHierarchyHelper.FlowContext context =
            helper.findParentFlowContext(topFlow, "level2");

        // Verify
        assertNotNull("Should find parent context", context);
        assertEquals("Parent should be level1", "level1", context.getParentFlow().getId());
        assertEquals("Execution should reference level2", "level2",
            context.getSubFlowExecution().getFlowId());
    }

    /**
     * Test: Finding parent context when child does not exist
     *
     * Expected: null (not found)
     */
    @Test
    public void testFindParentFlowContext_NotFound() {
        // Setup
        AuthenticationFlowModel topFlow = createFlow("top-flow", "TopFlow");
        realm.addFlow(topFlow);
        realm.addExecution("top-flow", createAuthenticatorExecution("cookie", "top-flow"));

        // Test
        AuthenticationFlowHierarchyHelper.FlowContext context =
            helper.findParentFlowContext(topFlow, "non-existent");

        // Verify
        assertNull("Should return null when child not found", context);
    }

    /**
     * Test: Disabled execution should be ignored
     *
     * <pre>
     * TopFlow
     *   ├─ SubFlow
     *   ├─ DisabledAuth (DISABLED) ← Should be ignored
     *   └─ EnabledAuth              ← Should be found
     * </pre>
     *
     * Expected: true (enabled execution found, disabled ignored)
     */
    @Test
    public void testDisabledExecution_ShouldBeIgnored() {
        // Setup
        AuthenticationFlowModel topFlow = createFlow("top-flow", "TopFlow");
        AuthenticationFlowModel subFlow = createFlow("sub-flow", "SubFlow");

        realm.addFlow(topFlow);
        realm.addFlow(subFlow);

        realm.addExecution("top-flow", createFlowExecution("sub-flow", "top-flow"));

        AuthenticationExecutionModel disabledExec = createAuthenticatorExecution("disabled-auth", "top-flow");
        disabledExec.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED);
        realm.addExecution("top-flow", disabledExec);

        realm.addExecution("top-flow", createAuthenticatorExecution("enabled-auth", "top-flow"));

        // Test
        boolean result = helper.hasMoreExecutionsAfterParentFlow(topFlow, "sub-flow");

        // Verify
        assertTrue("Should return true, skipping disabled execution", result);
    }

    // Helper methods for creating test data

    private AuthenticationFlowModel createFlow(String id, String alias) {
        AuthenticationFlowModel flow = new AuthenticationFlowModel();
        flow.setId(id);
        flow.setAlias(alias);
        return flow;
    }

    private AuthenticationExecutionModel createAuthenticatorExecution(String authenticator, String parentFlowId) {
        AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
        execution.setId(authenticator + "-exec-id");
        execution.setAuthenticator(authenticator);
        execution.setParentFlow(parentFlowId);
        execution.setAuthenticatorFlow(false);
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        return execution;
    }

    private AuthenticationExecutionModel createFlowExecution(String flowId, String parentFlowId) {
        AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
        execution.setId(flowId + "-exec-id");
        execution.setFlowId(flowId);
        execution.setParentFlow(parentFlowId);
        execution.setAuthenticatorFlow(true);
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        return execution;
    }

    /**
     * Simple test double for RealmModel that stores flows and executions in memory.
     * Uses dynamic proxy to avoid implementing all RealmModel methods.
     */
    private static class TestRealmModel {
        private final Map<String, AuthenticationFlowModel> flows = new HashMap<>();
        private final Map<String, List<AuthenticationExecutionModel>> executions = new HashMap<>();
        private final RealmModel proxy;

        TestRealmModel() {
            this.proxy = (RealmModel) java.lang.reflect.Proxy.newProxyInstance(
                RealmModel.class.getClassLoader(),
                new Class<?>[]{RealmModel.class},
                (proxy, method, args) -> {
                    // Handle getAuthenticationFlowById
                    if (method.getName().equals("getAuthenticationFlowById") && args != null && args.length == 1) {
                        return flows.get(args[0]);
                    }
                    // Handle getAuthenticationExecutionsStream
                    if (method.getName().equals("getAuthenticationExecutionsStream") && args != null && args.length == 1) {
                        return executions.getOrDefault(args[0], new ArrayList<>()).stream();
                    }
                    // All other methods throw UnsupportedOperationException
                    throw new UnsupportedOperationException("Method " + method.getName() + " not implemented in test double");
                }
            );
        }

        RealmModel getProxy() {
            return proxy;
        }

        void addFlow(AuthenticationFlowModel flow) {
            flows.put(flow.getId(), flow);
            executions.putIfAbsent(flow.getId(), new ArrayList<>());
        }

        void addExecution(String flowId, AuthenticationExecutionModel execution) {
            executions.computeIfAbsent(flowId, k -> new ArrayList<>()).add(execution);
        }
    }
}
