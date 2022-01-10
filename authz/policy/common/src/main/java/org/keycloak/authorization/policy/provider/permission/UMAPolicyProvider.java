/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authorization.policy.provider.permission;

import org.keycloak.authorization.identity.Identity;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.policy.evaluation.Evaluation;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class UMAPolicyProvider extends AbstractPermissionProvider {

    @Override
    public void evaluate(Evaluation evaluation) {
        ResourcePermission permission = evaluation.getPermission();
        Resource resource = permission.getResource();

        if (resource != null) {
            Identity identity = evaluation.getContext().getIdentity();

            // no need to evaluate UMA permissions to resource owner resources
            if (resource.getOwner().equals(identity.getId())) {
                evaluation.grant();
                return;
            }
        }

        super.evaluate(evaluation);
    }
}
