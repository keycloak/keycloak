/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
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
package org.keycloak.authorization.policy.provider.js;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.evaluation.Evaluation;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.models.RealmModel;
import org.keycloak.models.ScriptModel;
import org.keycloak.scripting.EvaluatableScriptAdapter;
import org.keycloak.scripting.ScriptingProvider;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class JSPolicyProvider implements PolicyProvider {

    @Override
    public void evaluate(Evaluation evaluation) {
        Policy policy = evaluation.getPolicy();

        AuthorizationProvider authorization = evaluation.getAuthorizationProvider();
        ScriptModel script = getScriptModel(policy, authorization);
        final EvaluatableScriptAdapter adapter = getScriptingProvider(authorization).prepareEvaluatableScript(script);

        try {
            //how to deal with long running scripts -> timeout?
            adapter.eval(bindings -> {
                bindings.put("script", adapter.getScriptModel());
                bindings.put("$evaluation", evaluation);
            });
        }
        catch (Exception e) {
            throw new RuntimeException("Error evaluating JS Policy [" + policy.getName() + "].", e);
        }
    }

    @Override
    public void close() {

    }

    private ScriptModel getScriptModel(final Policy policy, final AuthorizationProvider authorization) {
        String scriptName = policy.getName();
        String scriptCode = policy.getConfig().get("code");
        String scriptDescription = policy.getDescription();

        RealmModel realm = authorization.getRealm();

        //TODO lookup script by scriptId instead of creating it every time
        return getScriptingProvider(authorization).createScript(realm.getId(), ScriptModel.TEXT_JAVASCRIPT, scriptName, scriptCode, scriptDescription);
    }

    private ScriptingProvider getScriptingProvider(final AuthorizationProvider authorization) {
        return authorization.getKeycloakSession().getProvider(ScriptingProvider.class);
    }
}
