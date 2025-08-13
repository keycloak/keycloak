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

import java.util.List;
import org.keycloak.provider.Provider;

/**
 * TODO: Maybe we want to split the provider into two???
 * * Time based 
 *   ** UserCreationDatePolicyProvider, LastAuthenticationTimePolicyProvider ...
 * * Origin based
 *   ** IdpResourceFilterProvider, LdapResourceFilterProvider, AllResourceFilterProvider
 * 
 */
public interface ResourcePolicyProvider extends Provider {

    /**
     * Finds all resources that are eligible for the first action of a policy.
     *
     * @param time The time delay for the first action.
     * @return A list of eligible resource IDs.
     */
    List<String> getEligibleResourcesForInitialAction(long time);

    /** 
     * This method checks a list of candidates and returns only those that are eligible based on time.
     */ 
    List<String> filterEligibleResources(List<String> candidateResourceIds, long time);
}
