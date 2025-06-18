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

package org.keycloak.testsuite.util;


import org.keycloak.representations.idm.AuthenticationFlowRepresentation;

/**
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class FlowBuilder {

    private AuthenticationFlowRepresentation rep = new AuthenticationFlowRepresentation();

    public static FlowBuilder create() {
        return new FlowBuilder();
    }

    private FlowBuilder() {
    }

    public FlowBuilder id(String id) {
        rep.setId(id);
        return this;
    }

    public FlowBuilder alias(String alias) {
        rep.setAlias(alias);
        return this;
    }

    public FlowBuilder description(String description) {
        rep.setDescription(description);
        return this;
    }

    public FlowBuilder providerId(String providerId) {
        rep.setProviderId(providerId);
        return this;
    }

    public FlowBuilder topLevel(boolean topLevel) {
        rep.setTopLevel(topLevel);
        return this;
    }

    public FlowBuilder builtIn(boolean builtIn) {
        rep.setBuiltIn(builtIn);
        return this;
    }

    public AuthenticationFlowRepresentation build() {
        return rep;
    }

}
