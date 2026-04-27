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

package org.keycloak.testsuite.account.custom;

import java.util.List;
import java.util.function.Function;

import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;

import org.junit.Before;

/**
 *
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public abstract class AbstractCustomAccountManagementTest extends AbstractAccountManagementTest {

    private AuthenticationManagementResource authMgmtResource;
    
    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
    }
    
    @Before
    public void beforeTest() {
        authMgmtResource = testRealmResource().flows();
        createAppClientInRealm(testRealmResource().toRepresentation().getRealm());
    }
    
    protected AuthenticationManagementResource getAuthMgmtResource() {
        return authMgmtResource;
    }

    protected void updateRequirement(String flowAlias, String provider, AuthenticationExecutionModel.Requirement requirement) {
        AuthenticationExecutionInfoRepresentation exec = getExecution(flowAlias, provider);
        
        exec.setRequirement(requirement.name());
        authMgmtResource.updateExecutions(flowAlias, exec);
    }

    protected void updateRequirement(String flowAlias, AuthenticationExecutionModel.Requirement requirement, Function<AuthenticationExecutionInfoRepresentation, Boolean> filterFunc){
        List<AuthenticationExecutionInfoRepresentation> executionReps = authMgmtResource.getExecutions(flowAlias);
        AuthenticationExecutionInfoRepresentation exec =  executionReps.stream().filter(filterFunc::apply).findFirst().orElse(null);
        if (exec != null) {
            exec.setRequirement(requirement.name());
            authMgmtResource.updateExecutions(flowAlias, exec);
        }
    }
    
    protected AuthenticationExecutionInfoRepresentation getExecution(String flowAlias, String provider) {
        List<AuthenticationExecutionInfoRepresentation> executionReps = authMgmtResource.getExecutions(flowAlias);

        for (AuthenticationExecutionInfoRepresentation exec : executionReps) {
            if (provider.equals(exec.getProviderId())) {
                return exec;
            }
        }
        return null;
    }

}
