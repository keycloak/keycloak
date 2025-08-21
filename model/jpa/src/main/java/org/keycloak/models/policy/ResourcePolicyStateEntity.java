/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.policy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

/**
 * Represents the state of a resource within a time-based policy flow.
 */
@Entity
@Table(name = "RESOURCE_POLICY_STATE")
@IdClass(ResourcePolicyStateEntity.PrimaryKey.class)
public class ResourcePolicyStateEntity {

    @Id
    @Column(name = "RESOURCE_ID")
    private String resourceId;

    @Id
    @Column(name = "POLICY_ID")
    private String policyId;

    @Column(name = "RESOURCE_TYPE")
    private String resourceType; // do we want/need to store this?

    @Column(name = "POLICY_PROVIDER_ID")
    private String policyProviderId;

    @Column(name = "SCHEDULED_ACTION_ID")
    private String scheduledActionId;

    @Column(name = "SCHEDULED_ACTION_TIMESTAMP")
    private long scheduledActionTimestamp;

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public String getPolicyProviderId() {
        return policyProviderId;
    }

    public void setPolicyProviderId(String policyProviderId) {
        this.policyProviderId = policyProviderId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getScheduledActionId() {
        return scheduledActionId;
    }

    public void setScheduledActionId(String scheduledActionId) {
        this.scheduledActionId = scheduledActionId;
    }

    public long getScheduledActionTimestamp() {
        return scheduledActionTimestamp;
    }

    public void setScheduledActionTimestamp(long scheduledActionTimestamp) {
        this.scheduledActionTimestamp = scheduledActionTimestamp;
    }

    public static class PrimaryKey implements Serializable {

        private String resourceId;
        private String policyId;

        public PrimaryKey() {
        }

        public PrimaryKey(String resourceId, String policyId) {
            this.resourceId = resourceId;
            this.policyId = policyId;
        }

        public String getResourceId() {
            return resourceId;
        }

        public void setResourceId(String resourceId) {
            this.resourceId = resourceId;
        }

        public String getPolicyId() {
            return policyId;
        }

        public void setPolicyId(String policyId) {
            this.policyId = policyId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PrimaryKey that = (PrimaryKey) o;
            return Objects.equals(resourceId, that.resourceId) && Objects.equals(policyId, that.policyId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(resourceId, policyId);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourcePolicyStateEntity that = (ResourcePolicyStateEntity) o;
        return Objects.equals(resourceId, that.resourceId) && Objects.equals(policyId, that.policyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceId, policyId);
    }
}

