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

package org.keycloak.representations.idm;

import java.io.Serializable;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class AbstractAuthenticationExecutionRepresentation implements Serializable {

    private String authenticatorConfig;
    private String authenticator;
    private boolean authenticatorFlow;
    private String requirement;
    private Integer priority;

    public String getAuthenticatorConfig() {
        return authenticatorConfig;
    }

    public void setAuthenticatorConfig(String authenticatorConfig) {
        this.authenticatorConfig = authenticatorConfig;
    }

    public String getAuthenticator() {
        return authenticator;
    }

    public void setAuthenticator(String authenticator) {
        this.authenticator = authenticator;
    }

    public String getRequirement() {
        return requirement;
    }

    public void setRequirement(String requirement) {
        this.requirement = requirement;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    /**
     * Is the referenced authenticator a flow?
     *
     * @return
     */
    @Deprecated
    private boolean autheticatorFlow;

    @Deprecated
    public boolean isAutheticatorFlow() {
        return authenticatorFlow;
    }

    @Deprecated
    public void setAutheticatorFlow(boolean autheticatorFlow) {
        this.authenticatorFlow = autheticatorFlow;
    }

    /**
     * Is the referenced authenticator a flow?
     *
     * @return
     */
    public boolean isAuthenticatorFlow() {
        return authenticatorFlow;
    }

    public void setAuthenticatorFlow(boolean authenticatorFlow) {
        this.authenticatorFlow = authenticatorFlow;
    }

}
