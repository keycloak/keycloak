/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import org.keycloak.util.EnumWithStableIndex;

/**
* @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
* @version $Revision: 1 $
*/
public class AuthenticationExecutionModel implements Serializable {

    public static class ExecutionComparator implements Comparator<AuthenticationExecutionModel> {
        public static final ExecutionComparator SINGLETON = new ExecutionComparator();

        @Override
        public int compare(AuthenticationExecutionModel o1, AuthenticationExecutionModel o2) {
            return o1.priority - o2.priority;
        }
    }

    private String id;
    private String authenticatorConfig;
    private String authenticator;
    private String flowId;
    private boolean authenticatorFlow;
    private Requirement requirement;
    private int priority;
    private String parentFlow;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuthenticatorConfig() {
        return authenticatorConfig;
    }

    public void setAuthenticatorConfig(String authenticatorConfig) {
        this.authenticatorConfig = authenticatorConfig;
    }

    public String getAuthenticator() {
        return authenticator;
    }

    public void setAuthenticator(String authenticator) {
        this.authenticator = authenticator;
    }

    public Requirement getRequirement() {
        return requirement;
    }

    public void setRequirement(Requirement requirement) {
        this.requirement = requirement;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getParentFlow() {
        return parentFlow;
    }

    public void setParentFlow(String parentFlow) {
        this.parentFlow = parentFlow;
    }

    /**
     * If this execution is a flow, this is the flowId pointing to an AuthenticationFlowModel
     *
     * @return
     */
    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    /**
     * Is the referenced authenticator a flow?
     *
     * @return
     */
    public boolean isAuthenticatorFlow() {
        return authenticatorFlow;
    }

    public void setAuthenticatorFlow(boolean authenticatorFlow) {
        this.authenticatorFlow = authenticatorFlow;
    }

    public enum Requirement implements EnumWithStableIndex {
        REQUIRED(0),
        CONDITIONAL(1),
        ALTERNATIVE(2),
        DISABLED(3);

        private final int stableIndex;
        private static final Map<Integer, Requirement> BY_ID = EnumWithStableIndex.getReverseIndex(values());

        private Requirement(int stableIndex) {
            Objects.requireNonNull(stableIndex);
            this.stableIndex = stableIndex;
        }

        @Override
        public int getStableIndex() {
            return stableIndex;
        }

        public static Requirement valueOfInteger(Integer id) {
            return id == null ? null : BY_ID.get(id);
        }
    }

    public boolean isRequired() {
        return requirement == Requirement.REQUIRED;
    }
    public boolean isConditional() {
        return requirement == Requirement.CONDITIONAL;
    }
    public boolean isAlternative() {
        return requirement == Requirement.ALTERNATIVE;
    }
    public boolean isDisabled() {
        return requirement == Requirement.DISABLED;
    }
    public boolean isEnabled() {
        return requirement != Requirement.DISABLED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AuthenticationExecutionModel that = (AuthenticationExecutionModel) o;

        if (id == null || that.id == null) return false;
        return id.equals(that.id);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
