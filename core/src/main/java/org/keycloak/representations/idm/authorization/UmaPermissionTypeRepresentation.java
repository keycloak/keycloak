/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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
package org.keycloak.representations.idm.authorization;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Set;

/**
 * @author <a href="mailto:federico@martel-innovate.com">Federico M. Facca</a>
 */

public class UmaPermissionTypeRepresentation {
    
    private Logic logic = Logic.POSITIVE;
    private DecisionStrategy decisionStrategy = DecisionStrategy.UNANIMOUS;
    private Set<String> scopes;
    @JsonIgnore
    public static String[] supportedPolicies = {"user","group","client","role"};
    private Set<UmaPolicyRepresentation> policies;
    private String id;
    
    public void setId(String id){
        this.id = id;
    }
    
    public String getId(){
        return id;
    }
    
    public Set<UmaPolicyRepresentation> getPolicies() {
        return policies;
    }
    
    public void setPolicies(Set<UmaPolicyRepresentation> policies) {
        this.policies = policies;
    }
    
    public DecisionStrategy getDecisionStrategy() {
        return this.decisionStrategy;
    }

    public void setDecisionStrategy(DecisionStrategy decisionStrategy) {
        this.decisionStrategy = decisionStrategy;
    }

    public Logic getLogic() {
        return logic;
    }

    public void setLogic(Logic logic) {
        this.logic = logic;
    }
    
    public Set<String> getScopes() {
        return scopes;
    }

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }

}
