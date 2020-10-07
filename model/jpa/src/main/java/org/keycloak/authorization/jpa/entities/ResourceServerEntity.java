/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authorization.jpa.entities;

import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.PolicyEnforcementMode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@Entity
@Table(name = "RESOURCE_SERVER")
public class ResourceServerEntity {

    @Id
    @Column(name="ID", length = 36)
    private String id;

    @Column(name = "ALLOW_RS_REMOTE_MGMT")
    private boolean allowRemoteResourceManagement;

    @Column(name = "POLICY_ENFORCE_MODE")
    private PolicyEnforcementMode policyEnforcementMode = PolicyEnforcementMode.ENFORCING;

    @Column(name = "DECISION_STRATEGY")
    private DecisionStrategy decisionStrategy = DecisionStrategy.UNANIMOUS;

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isAllowRemoteResourceManagement() {
        return this.allowRemoteResourceManagement;
    }

    public void setAllowRemoteResourceManagement(boolean allowRemoteResourceManagement) {
        this.allowRemoteResourceManagement = allowRemoteResourceManagement;
    }

    public PolicyEnforcementMode getPolicyEnforcementMode() {
        return this.policyEnforcementMode;
    }

    public void setPolicyEnforcementMode(PolicyEnforcementMode policyEnforcementMode) {
        this.policyEnforcementMode = policyEnforcementMode;
    }

    public void setDecisionStrategy(DecisionStrategy decisionStrategy) {
        if (DecisionStrategy.CONSENSUS.equals(decisionStrategy)) {
            throw new IllegalArgumentException("Strategy " + decisionStrategy + " not supported");
        }
        this.decisionStrategy = decisionStrategy;
    }

    public DecisionStrategy getDecisionStrategy() {
        return decisionStrategy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourceServerEntity that = (ResourceServerEntity) o;

        return getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
