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

package org.keycloak.authentication.authenticators.console;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.ConsoleDisplayMode;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticator;
import org.keycloak.models.credential.OTPCredentialModel;

import javax.ws.rs.core.Response;
import java.net.URI;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ConsoleOTPFormAuthenticator extends OTPFormAuthenticator implements Authenticator {
    public static final ConsoleOTPFormAuthenticator SINGLETON = new ConsoleOTPFormAuthenticator();

    public static URI getCallbackUrl(AuthenticationFlowContext context) {
        return context.getActionUrl(context.generateAccessCode(), true);
    }

    protected ConsoleDisplayMode challenge(AuthenticationFlowContext context) {
        return ConsoleDisplayMode.challenge(context)
                .header()
                .param(OTPCredentialModel.TYPE)
                .label("console-otp")
                 .challenge();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        validateOTP(context);
    }



    @Override
    public void authenticate(AuthenticationFlowContext context) {
        Response challengeResponse = challenge(context, null);
        context.challenge(challengeResponse);
    }

    @Override
    protected Response challenge(AuthenticationFlowContext context, String msg) {
        if (msg == null) {
            return challenge(context).response();
        }
        return challenge(context).message(msg);
    }

    @Override
    public void close() {

    }
}
