/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authentication.requiredactions;

import java.util.List;

import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.broker.provider.IdpLinkAction;

import org.junit.Assert;
import org.junit.Test;

public class RequiredActionOneTimeActionTest {

    @Test
    public void testBuiltInRequiredActionsAreOneTime() {
        List<RequiredActionFactory> requiredActions = List.of(
                new DeleteAccount(),
                new DeleteCredentialAction(),
                new IdpLinkAction(),
                new RecoveryAuthnCodesAction(),
                new TermsAndConditions(),
                new UpdateEmail(),
                new UpdatePassword(),
                new UpdateProfile(),
                new UpdateTotp(),
                new UpdateUserLocaleAction(),
                new VerifyEmail(),
                new VerifyUserProfile(),
                new WebAuthnRegisterFactory(),
                new WebAuthnPasswordlessRegisterFactory()
        );

        for (RequiredActionFactory requiredAction : requiredActions) {
            Assert.assertTrue("Expected one-time action: " + requiredAction.getClass().getSimpleName(), requiredAction.isOneTimeAction());
        }
    }
}
