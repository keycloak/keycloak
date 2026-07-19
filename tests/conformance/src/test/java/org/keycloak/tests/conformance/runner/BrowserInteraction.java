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

package org.keycloak.tests.conformance.runner;

import java.util.List;

import org.keycloak.tests.conformance.runner.BrowserFlow.BrowserTask;

public interface BrowserInteraction {

    BrowserInteraction NONE = new None();
    BrowserInteraction LOGIN = new Login();

    static BrowserInteraction errorPage(String messageRegexp) {
        return new ErrorPage(messageRegexp);
    }

    List<BrowserFlow> browserFlows(BrowserContext context);

    record BrowserContext(String realm, String username, String password, String callbackUrl) {

        String authorizationEndpoint() {
            return "https://*/realms/" + realm + "/protocol/openid-connect/auth*";
        }

        String loginPage() {
            return "https://*/realms/" + realm + "/login-actions/authenticate*";
        }
    }

    record None() implements BrowserInteraction {

        @Override
        public List<BrowserFlow> browserFlows(BrowserContext context) {
            return List.of();
        }
    }

    record Login() implements BrowserInteraction {

        @Override
        public List<BrowserFlow> browserFlows(BrowserContext context) {
            return List.of(new BrowserFlow(context.authorizationEndpoint(), List.of(
                    new BrowserFlow.BrowserTask(
                            "Keycloak Login",
                            context.loginPage(),
                            List.of(
                                    // command, element selector type, element selector, value to enter
                                    List.of(BrowserTask.TEXT, "id", "username", context.username()),
                                    List.of(BrowserTask.TEXT, "id", "password", context.password()),
                                    // command, element selector type, element selector
                                    List.of(BrowserTask.CLICK, "id", "kc-login"))),
                    new BrowserFlow.BrowserTask(
                            "Verify Complete",
                            context.callbackUrl() + "*",
                            // command, element selector type, element selector, timeout in seconds
                            List.of(List.of(BrowserTask.WAIT, "id", "submission_complete", 10))))));
        }
    }

    record ErrorPage(String messageRegexp) implements BrowserInteraction {

        @Override
        public List<BrowserFlow> browserFlows(BrowserContext context) {
            return List.of(new BrowserFlow(context.authorizationEndpoint(), List.of(
                    new BrowserFlow.BrowserTask(
                            "Keycloak Error Page",
                            context.authorizationEndpoint(),
                            // command, element selector type, element selector, timeout in seconds, element text regexp, action
                            List.of(List.of(BrowserTask.WAIT, "id", "kc-error-message", 10, messageRegexp,
                                    BrowserTask.UPDATE_IMAGE_PLACEHOLDER))))));
        }
    }
}
