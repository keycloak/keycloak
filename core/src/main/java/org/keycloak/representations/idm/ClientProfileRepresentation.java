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

package org.keycloak.representations.idm;

import java.util.List;
import java.util.Objects;

/**
 * Client Profile's external representation class
 *
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ClientProfileRepresentation {

    protected String name;
    protected String description;
    protected List<ClientPolicyExecutorRepresentation> executors;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ClientPolicyExecutorRepresentation> getExecutors() {
        return executors;
    }

    public void setExecutors(List<ClientPolicyExecutorRepresentation> executors) {
        this.executors = executors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientProfileRepresentation that = (ClientProfileRepresentation) o;
        return Objects.equals(name, that.name) && Objects.equals(description, that.description) && Objects.equals(executors, that.executors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, executors);
    }
}
