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

package org.keycloak.authentication;

/**
 * Status of an execution/authenticator in a Authentication Flow
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public enum FlowStatus {
    /**
     * Successful execution
     */
    SUCCESS,

    /**
     * Execution offered a challenge.  Optional executions will ignore this challenge.  Alternative executions may
     * ignore the challenge depending on the status of other executions in the flow.
     *
     */
    CHALLENGE,

    /**
     * Regardless of the execution's requirement, this challenge will be sent to the user.
     *
     */
    FORCE_CHALLENGE,

    /**
     * Flow will be aborted and a Response provided by the execution will be sent.
     *
     */
    FAILURE_CHALLENGE,

    /**
     * Flow will be aborted.
     *
     */
    FAILED,

    /**
     * This is not an error condition.  Execution was attempted, but the authenticator is unable to process the request.  An example of this is if
     * a Kerberos authenticator did not see a negotiate header.  There was no error, but the execution was attempted.
     *
     */
    ATTEMPTED,

    /**
     * This flow is being forked.  The current authentication session is being cloned, reset, and redirected to browser login.
     *
     */
    FORK,

    /**
     * This flow was reset to the beginning.  An example is hitting cancel on the OTP page which will bring you back to the
     * username password page.
     *
     */
    FLOW_RESET

}
