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
    BrowserInteraction DENY_CONSENT = new DenyConsent();
    BrowserInteraction LOGIN_ON_SECOND_VISIT = new LoginOnSecondVisit();
    BrowserInteraction ERROR_CALLBACK = new ErrorCallback();

    /**
     * Asserts Keycloak renders its error page with a message matching {@code messageRegexp} at the authorization
     * endpoint (no login).
     */
    static BrowserInteraction errorPage(String messageRegexp) {
        return new ErrorPage(messageRegexp);
    }

    /**
     * Logs in and completes the first authorization, then asserts the second authorization is rejected with
     * Keycloak's error page matching {@code messageRegexp}.
     */
    static BrowserInteraction loginThenErrorPageOnSecondVisit(String messageRegexp) {
        return new LoginThenErrorPageOnSecondVisit(messageRegexp);
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

    private static BrowserTask loginTask(BrowserContext context, boolean optional) {
        return new BrowserTask(
                "Keycloak Login",
                context.loginPage(),
                optional,
                List.of(
                        // command, element selector type, element selector, value to enter
                        List.of(BrowserTask.TEXT, "id", "username", context.username()),
                        List.of(BrowserTask.TEXT, "id", "password", context.password()),
                        // command, element selector type, element selector
                        List.of(BrowserTask.CLICK, "id", "kc-login")));
    }

    private static BrowserTask verifyCompleteTask(BrowserContext context) {
        return new BrowserTask(
                "Verify Complete",
                context.callbackUrl() + "*",
                // command, element selector type, element selector, timeout in seconds
                List.of(List.of(BrowserTask.WAIT, "id", "submission_complete", 10)));
    }

    private static BrowserTask errorPageTask(BrowserContext context, String messageRegexp) {
        return new BrowserTask(
                "Keycloak Error Page",
                context.authorizationEndpoint(),
                // command, element selector type, element selector, timeout in seconds, element text regexp, action
                List.of(List.of(BrowserTask.WAIT, "id", "kc-error-message", 10, messageRegexp,
                        BrowserTask.UPDATE_IMAGE_PLACEHOLDER)));
    }

    record None() implements BrowserInteraction {

        @Override
        public List<BrowserFlow> browserFlows(BrowserContext context) {
            return List.of();
        }
    }

    /**
     * Requires a login on the first authorization visit. Later visits reuse the SSO session, where Keycloak may
     * skip the login page, so the login task is optional there.
     */
    record Login() implements BrowserInteraction {

        @Override
        public List<BrowserFlow> browserFlows(BrowserContext context) {
            BrowserFlow firstVisit = new BrowserFlow(context.authorizationEndpoint(), 1, List.of(
                    loginTask(context, false),
                    verifyCompleteTask(context)));
            BrowserFlow laterVisits = new BrowserFlow(context.authorizationEndpoint(), List.of(
                    loginTask(context, true),
                    verifyCompleteTask(context)));
            return List.of(firstVisit, laterVisits);
        }
    }

    record DenyConsent() implements BrowserInteraction {

        @Override
        public List<BrowserFlow> browserFlows(BrowserContext context) {
            return List.of(new BrowserFlow(context.authorizationEndpoint(), List.of(
                    // optional so a second authorization that reuses the SSO session skips the login
                    loginTask(context, true),
                    new BrowserFlow.BrowserTask(
                            "Deny Consent",
                            // the consent (oauth grant) screen is served under login-actions during the auth flow,
                            // optional so a second authorization that goes straight to the callback is tolerated
                            "https://*/realms/" + context.realm() + "/login-actions/*",
                            true,
                            List.of(
                                    // wait for the deny button, then click it to reject the grant
                                    List.of(BrowserTask.WAIT, "id", "kc-cancel", 10),
                                    List.of(BrowserTask.CLICK, "id", "kc-cancel"))),
                    // the suite renders submission_complete on the callback for the access_denied response too
                    verifyCompleteTask(context))));
        }
    }

    /**
     * Completes a normal login on the first authorization visit, then requires Keycloak's error page (matching
     * {@code messageRegexp}) on the second visit. The first flow uses match-limit 1 so it applies only once; the
     * second flow handles every later visit.
     */
    record LoginThenErrorPageOnSecondVisit(String messageRegexp) implements BrowserInteraction {

        @Override
        public List<BrowserFlow> browserFlows(BrowserContext context) {
            BrowserFlow firstVisit = new BrowserFlow(context.authorizationEndpoint(), 1, List.of(
                    loginTask(context, false),
                    verifyCompleteTask(context)));
            BrowserFlow secondVisit = new BrowserFlow(context.authorizationEndpoint(), List.of(
                    errorPageTask(context, messageRegexp)));
            return List.of(firstVisit, secondVisit);
        }
    }

    /**
     * Loads the login page on the first authorization visit without authenticating, then logs in on the second
     * visit. The first flow uses match-limit 1 so it applies only once, the second flow handles every later
     * visit. This keeps the first authorization incomplete so a request_uri stays valid for the second visit.
     */
    record LoginOnSecondVisit() implements BrowserInteraction {

        @Override
        public List<BrowserFlow> browserFlows(BrowserContext context) {
            BrowserFlow firstVisit = new BrowserFlow(context.authorizationEndpoint(), 1, List.of(
                    new BrowserFlow.BrowserTask(
                            "Load login page without authenticating",
                            context.loginPage(),
                            // wait for the form to confirm the request_uri was accepted, but do not submit it
                            List.of(List.of(BrowserTask.WAIT, "id", "username", 10)))));
            BrowserFlow secondVisit = new BrowserFlow(context.authorizationEndpoint(), List.of(
                    loginTask(context, false),
                    verifyCompleteTask(context)));
            return List.of(firstVisit, secondVisit);
        }
    }

    record ErrorPage(String messageRegexp) implements BrowserInteraction {

        @Override
        public List<BrowserFlow> browserFlows(BrowserContext context) {
            return List.of(new BrowserFlow(context.authorizationEndpoint(), List.of(
                    errorPageTask(context, messageRegexp))));
        }
    }

    /**
     * Asserts Keycloak rejects the authorization request without any login by redirecting the error response
     * straight to the callback, where the module validates it.
     */
    record ErrorCallback() implements BrowserInteraction {

        @Override
        public List<BrowserFlow> browserFlows(BrowserContext context) {
            return List.of(new BrowserFlow(context.authorizationEndpoint(), List.of(
                    verifyCompleteTask(context))));
        }
    }
}
