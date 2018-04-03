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

package org.keycloak.authentication.requiredactions;

import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.ConsoleDisplayMode;
import org.keycloak.common.util.Time;

import javax.ws.rs.core.Response;
import java.util.Arrays;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ConsoleTermsAndConditions implements RequiredActionProvider {
    public static final ConsoleTermsAndConditions SINGLETON = new ConsoleTermsAndConditions();
    public static final String USER_ATTRIBUTE = TermsAndConditions.PROVIDER_ID;

    @Override
    public void evaluateTriggers(RequiredActionContext context) {

    }


    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        Response challenge = ConsoleDisplayMode.challenge(context)
                .header()
                .param("accept")
                .label("console-accept-terms")
                .message("termsPlainText");
        context.challenge(challenge);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        String accept = context.getHttpRequest().getDecodedFormParameters().getFirst("accept");

        String yes = context.form().getMessage("console-accept");

        if (!accept.equals(yes)) {
            context.getUser().removeAttribute(USER_ATTRIBUTE);
            requiredActionChallenge(context);
            return;
        }

        context.getUser().setAttribute(USER_ATTRIBUTE, Arrays.asList(Integer.toString(Time.currentTime())));

        context.success();
    }

    @Override
    public void close() {

    }
}
