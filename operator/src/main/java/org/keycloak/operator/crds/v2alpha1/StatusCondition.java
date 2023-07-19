/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.operator.crds.v2alpha1;

import io.fabric8.kubernetes.api.model.AnyType;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StatusCondition {
    public enum Status {
        True,
        False,
        Unknown
    }

    private String type;
    private JsonNode status = TextNode.valueOf(Status.Unknown.name());
    private String message;
    private String lastTransitionTime;
    private Long observedGeneration;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @JsonIgnore
    public Boolean getStatus() {
        if (status == null || status.isNull()) {
            return null;
        }
        // account for the legacy boolean string as well
        switch (status.asText()) {
        case "false":
        case "False":
            return false;
        case "true":
        case "True":
            return true;
        default:
            return null;
        }
    }

    @JsonProperty("status")
    public String getStatusString() {
        if (status == null || status.isNull()) {
            return null;
        }
        return status.asText();
    }

    @JsonProperty("status")
    public void setStatusString(String status) {
        this.status = TextNode.valueOf(status);
    }

    @JsonIgnore
    public void setStatus(Boolean status) {
        if (status == null) {
            this.status = TextNode.valueOf(Status.Unknown.name());
        } else if (status) {
            this.status = TextNode.valueOf(Status.True.name());
        } else {
            this.status = TextNode.valueOf(Status.False.name());
        }
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLastTransitionTime() {
        return lastTransitionTime;
    }

    public void setLastTransitionTime(String lastTransitionTime) {
        this.lastTransitionTime = lastTransitionTime;
    }

    public Long getObservedGeneration() {
        return observedGeneration;
    }

    public void setObservedGeneration(Long observedGeneration) {
        this.observedGeneration = observedGeneration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StatusCondition that = (StatusCondition) o;
        return Objects.equals(getType(), that.getType()) && Objects.equals(getStatus(), that.getStatus()) && Objects.equals(getMessage(), that.getMessage())
                && Objects.equals(getLastTransitionTime(), that.getLastTransitionTime())
                && Objects.equals(getObservedGeneration(), that.getObservedGeneration());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType(), getStatus(), getMessage(), getObservedGeneration(), getLastTransitionTime());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "type='" + type + '\'' +
                ", status=" + status +
                ", message='" + message + '\'' +
                '}';
    }

}
