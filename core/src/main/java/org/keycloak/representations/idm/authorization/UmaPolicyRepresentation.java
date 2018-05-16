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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author <a href="mailto:federico@martel-innovate.com">Federico M. Facca</a>
 */

public class UmaPolicyRepresentation extends AbstractPolicyRepresentation {

    private Set<String> subjects;
    private Map<String,List<String>> options;
     
    public Set<String> getSubjects(){
        return this.subjects;
    }
    
    public Map<String,List<String>> getOptions(){
        return options;
    }
    
    public void setSubjects(Set<String> subjects){
        this.subjects = subjects;
    }
    
    public void setOptions(Map<String,List<String>> options){
        this.options = options;
    }
    
    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final UmaPolicyRepresentation policy = (UmaPolicyRepresentation) o;
        if (getId() != null && policy.getId() != null )
            return Objects.equals(getId(), policy.getId());
        return (subjects.equals(policy.getSubjects()) && options.equals(policy.getOptions()) && getType().equals(policy.getType()));
    }
    
}
