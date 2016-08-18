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

package org.keycloak.representations.idm;

import java.io.Serializable;
import java.util.List;

/**
* @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
* @version $Revision: 1 $
*/
public class AuthenticationExecutionInfoRepresentation implements Serializable {

    protected String id;
    protected String requirement;
    protected String displayName;
    protected String alias;
    protected List<String> requirementChoices;
    protected Boolean configurable;
    protected Boolean authenticationFlow;
    protected String providerId;
    protected String authenticationConfig;
    protected String flowId;
    protected int level;
    protected int index;

    public String getId() {
        return id;
    }

    public void setId(String execution) {
        this.id = execution;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getRequirement() {
        return requirement;
    }

    public void setRequirement(String requirement) {
        this.requirement = requirement;
    }

    public List<String> getRequirementChoices() {
        return requirementChoices;
    }

    public void setRequirementChoices(List<String> requirementChoices) {
        this.requirementChoices = requirementChoices;
    }

    public Boolean getConfigurable() {
        return configurable;
    }

    public void setConfigurable(Boolean configurable) {
        this.configurable = configurable;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getAuthenticationConfig() {
        return authenticationConfig;
    }

    public void setAuthenticationConfig(String authenticationConfig) {
        this.authenticationConfig = authenticationConfig;
    }

    public Boolean getAuthenticationFlow() {
        return authenticationFlow;
    }

    public void setAuthenticationFlow(Boolean authenticationFlow) {
        this.authenticationFlow = authenticationFlow;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }
}
