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

package org.keycloak.testframework.realm;


import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;

/**
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class AuthenticationExecutionBuilder extends Builder<AuthenticationExecutionRepresentation> {

    private AuthenticationExecutionBuilder(AuthenticationExecutionRepresentation rep) {
        super(rep);
    }

    public static AuthenticationExecutionBuilder create() {
        return new AuthenticationExecutionBuilder(new AuthenticationExecutionRepresentation());
    }

    public AuthenticationExecutionBuilder id(String id) {
        rep.setId(id);
        return this;
    }

    public AuthenticationExecutionBuilder parentFlow(String parentFlow) {
        rep.setParentFlow(parentFlow);
        return this;
    }

    public AuthenticationExecutionBuilder requirement(String requirement) {
        rep.setRequirement(requirement);
        return this;
    }

    public AuthenticationExecutionBuilder authenticator(String authenticator) {
        rep.setAuthenticator(authenticator);
        return this;
    }

    public AuthenticationExecutionBuilder priority(int priority) {
        rep.setPriority(priority);
        return this;
    }

    public AuthenticationExecutionBuilder authenticatorFlow(boolean authenticatorFlow) {
        rep.setAuthenticatorFlow(authenticatorFlow);
        return this;
    }

}
