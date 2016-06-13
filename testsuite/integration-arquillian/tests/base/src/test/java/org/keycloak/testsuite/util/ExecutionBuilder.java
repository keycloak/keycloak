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


import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;

/**
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class ExecutionBuilder {

    private AuthenticationExecutionRepresentation rep = new AuthenticationExecutionRepresentation();

    public static ExecutionBuilder create() {
        return new ExecutionBuilder();
    }

    private ExecutionBuilder() {
    }

    public ExecutionBuilder id(String id) {
        rep.setId(id);
        return this;
    }

    public ExecutionBuilder parentFlow(String parentFlow) {
        rep.setParentFlow(parentFlow);
        return this;
    }

    public ExecutionBuilder requirement(String requirement) {
        rep.setRequirement(requirement);
        return this;
    }

    public ExecutionBuilder authenticator(String authenticator) {
        rep.setAuthenticator(authenticator);
        return this;
    }

    public ExecutionBuilder priority(int priority) {
        rep.setPriority(priority);
        return this;
    }

    public ExecutionBuilder authenticatorFlow(boolean authenticatorFlow) {
        rep.setAutheticatorFlow(authenticatorFlow);
        return this;
    }

    public AuthenticationExecutionRepresentation build() {
        return rep;
    }

}
