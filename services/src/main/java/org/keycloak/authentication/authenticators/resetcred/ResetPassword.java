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

package org.keycloak.authentication.authenticators.resetcred;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;


/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResetPassword extends AbstractSetRequiredActionAuthenticator {

    public static final String PROVIDER_ID = "reset-password";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        if (context.getExecution().isRequired() ||
                (context.getExecution().isConditional() &&
                        configuredFor(context))) {
            context.getAuthenticationSession().addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
        }
        context.success();
    }

    protected boolean configuredFor(AuthenticationFlowContext context) {
        return context.getUser().credentialManager().isConfiguredFor(PasswordCredentialModel.TYPE);
    }

    @Override
    public String getDisplayType() {
        return "Reset Password";
    }

    @Override
    public String getHelpText() {
        return "Sets the Update Password required action if execution is REQUIRED.  Will also set it if execution is OPTIONAL and the password is currently configured for it.";
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
