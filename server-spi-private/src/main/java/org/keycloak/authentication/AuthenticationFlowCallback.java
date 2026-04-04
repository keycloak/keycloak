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
 *
 */

package org.keycloak.authentication;

import org.keycloak.models.AuthenticationFlowModel;

/**
 * Callback to be triggered during various lifecycle events of authentication flow.
 *
 * The {@link AuthenticatorFactory}, which creates this Authenticator should implement {@link AuthenticationFlowCallbackFactory} interface.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface AuthenticationFlowCallback extends Authenticator {

    /**
     * Triggered after the authentication flow is successfully finished. The target authentication flow is the one where this
     * authenticator is configured. Authenticator should finish successfully in the flow (or being evaluated to true in case of Conditional Authenticator)
     * in order to trigger this callback at the successful end of the flow
     *
     * @param context which encapsulate various useful data
     */
    void onParentFlowSuccess(AuthenticationFlowContext context);


    /**
     * Triggered after the top authentication flow is successfully finished.
     * It is really suitable for last verification of successful authentication
     *
     * @param topFlow which was successfully finished
     */
    default void onTopFlowSuccess(AuthenticationFlowModel topFlow) {
    }
}
