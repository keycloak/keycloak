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

import java.util.function.BiFunction;
import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.evaluation.Evaluation;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.scripting.EvaluatableScriptAdapter;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
class JSPolicyProvider implements PolicyProvider {

    private static final Logger logger = Logger.getLogger(JSPolicyProvider.class);

    private final BiFunction<AuthorizationProvider, Policy, EvaluatableScriptAdapter> evaluatableScript;

    JSPolicyProvider(final BiFunction<AuthorizationProvider, Policy, EvaluatableScriptAdapter> evaluatableScript) {
        this.evaluatableScript = evaluatableScript;
    }

    @Override
    public void evaluate(Evaluation evaluation) {
        Policy policy = evaluation.getPolicy();
        AuthorizationProvider authorization = evaluation.getAuthorizationProvider();
        EvaluatableScriptAdapter adapter = evaluatableScript.apply(authorization, policy);

        try {
            SimpleScriptContext context = new SimpleScriptContext();

            context.setAttribute("$evaluation", evaluation, ScriptContext.ENGINE_SCOPE);

            adapter.eval(context);
            logger.debugf("JS Policy %s evaluated to status %s", policy.getName(), evaluation.getEffect());
        }
        catch (Exception e) {
            throw new RuntimeException("Error evaluating JS Policy [" + policy.getName() + "].", e);
        }
    }

    @Override
    public void close() {
    }
}
