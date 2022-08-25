/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authorization.policy.provider.js;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.ScriptModel;
import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;
import org.keycloak.representations.provider.ScriptProviderMetadata;
import org.keycloak.scripting.ScriptingProvider;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public final class DeployedScriptPolicyFactory extends JSPolicyProviderFactory {

    private ScriptProviderMetadata metadata;

    public DeployedScriptPolicyFactory(ScriptProviderMetadata metadata) {
        this.metadata = metadata;
    }

    public DeployedScriptPolicyFactory() {
        // for reflection
    }

    @Override
    public String getId() {
        return metadata.getId();
    }

    @Override
    public String getName() {
        return metadata.getName();
    }

    @Override
    protected boolean isDeployed() {
        return true;
    }

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public JSPolicyRepresentation toRepresentation(Policy policy, AuthorizationProvider authorization) {
        JSPolicyRepresentation representation = new JSPolicyRepresentation();

        representation.setId(policy.getId());
        representation.setName(policy.getName());
        if (policy.getDescription() == null) {
            representation.setDescription(metadata.getDescription());
        }
        representation.setType(getId());
        representation.setCode(metadata.getCode());

        return representation;
    }

    @Override
    protected ScriptModel getScriptModel(Policy policy, RealmModel realm, ScriptingProvider scripting) {
        return scripting.createScript(realm.getId(), ScriptModel.TEXT_JAVASCRIPT, metadata.getName(), metadata.getCode(),
                metadata.getDescription());
    }

    @Override
    public void onCreate(Policy policy, JSPolicyRepresentation representation, AuthorizationProvider authorization) {
        if (representation.getDescription() == null) {
            representation.setDescription(metadata.getDescription());
            policy.setDescription(metadata.getDescription());
        }
        super.onCreate(policy, representation, authorization);
    }

    public ScriptProviderMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ScriptProviderMetadata metadata) {
        this.metadata = metadata;
    }
}
