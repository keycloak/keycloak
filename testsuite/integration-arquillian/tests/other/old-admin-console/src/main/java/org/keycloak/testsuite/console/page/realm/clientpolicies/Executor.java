/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.console.page.realm.clientpolicies;

import org.jboss.arquillian.graphene.page.Page;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class Executor extends BaseClientPoliciesPage {
    private static final String PROFILE_NAME = "profileName";
    private static final String EXECUTOR_INDEX = "executorIndex";

    @Page
    private ExecutorForm form;

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/profiles-update/{" + PROFILE_NAME + "}/update-executor/{" + EXECUTOR_INDEX + "}";
    }

    public void setUriParameters(String profileName, Integer executorIndex) {
        setUriParameter(PROFILE_NAME, profileName);
        setUriParameter(EXECUTOR_INDEX, executorIndex.toString());
    }

    public ExecutorForm form() {
        return form;
    }
}
