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

package org.keycloak.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Output of credential validation
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CredentialValidationOutput {

    private final UserModel authenticatedUser; // authenticated user.
    private final Status authStatus;           // status whether user is authenticated or more steps needed
    private final Map<String, String> state;   // Additional state related to authentication. It can contain data to be sent back to client or data about used credentials.

    public CredentialValidationOutput(UserModel authenticatedUser, Status authStatus, Map<String, String> state) {
        this.authenticatedUser = authenticatedUser;
        this.authStatus = authStatus;
        this.state = state;
    }

    public static CredentialValidationOutput failed() {
        return new CredentialValidationOutput(null, CredentialValidationOutput.Status.FAILED, new HashMap<>());
    }

    public static CredentialValidationOutput fallback() {
        return new CredentialValidationOutput(null, CredentialValidationOutput.Status.FALLBACK, new HashMap<>());
    }

    public UserModel getAuthenticatedUser() {
        return authenticatedUser;
    }

    public Status getAuthStatus() {
        return authStatus;
    }

    /**
     * State that is passed back by provider
     *
     * @return
     */
    public Map<String, String> getState() {
        return state;
    }

    public CredentialValidationOutput merge(CredentialValidationOutput that) {
        throw new IllegalStateException("Not supported yet");
    }

    public enum Status {

        /**
         * User was successfully authenticated. The {@link #getAuthenticatedUser()} must return authenticated user when this is used
         */
        AUTHENTICATED,

        /**
         * Federation provider failed to authenticate user. This is typically used when user storage provider recognizes the user, but credentials
         * are incorrect, so federation provider can mark whole authentication as not successful without eventual fallback to other user storage provider
         */
        FAILED,

        /**
         * Federation provider was not able to recognize the user. It is possible that credential was valid, but fereration provider was not able to lookup the user in it's storage.
         * Fallback to other user storage provider in the chain might be possible
         */
        FALLBACK,

        /**
         * Federation provider did not fully authenticate user. It may be needed to ask user for further challenge to then re-try authentication with same federation provider
         */
        CONTINUE,
    }
}
