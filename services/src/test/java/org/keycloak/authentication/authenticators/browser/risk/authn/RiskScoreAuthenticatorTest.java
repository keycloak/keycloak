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

package org.keycloak.authentication.authenticators.browser.risk.authn;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;

import org.junit.Test;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticator;
import org.keycloak.authentication.authenticators.browser.risk.context.LoginContext;
import org.keycloak.authentication.authenticators.browser.risk.context.LoginContextCollector;
import org.keycloak.authentication.authenticators.browser.risk.decision.AdaptiveAuthDecisionService;
import org.keycloak.authentication.authenticators.browser.risk.decision.AdaptiveAuthPolicy;
import org.keycloak.authentication.authenticators.browser.risk.decision.AuthAction;
import org.keycloak.authentication.authenticators.browser.risk.scoring.RiskFactorResult;
import org.keycloak.authentication.authenticators.browser.risk.scoring.RiskLevel;
import org.keycloak.authentication.authenticators.browser.risk.scoring.RiskScore;
import org.keycloak.authentication.authenticators.browser.risk.scoring.RiskScoringService;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.tracing.TracingProvider;

import io.opentelemetry.api.trace.Span;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class RiskScoreAuthenticatorTest {

    @Test
    public void lowRiskAddsEventDetailsAndSucceeds() {
        TestContext context = new TestContext(user("user-1"));
        RiskScore score = score(12, RiskLevel.LOW, List.of(
                RiskFactorResult.raw("failed_attempts", 0, Map.of()),
                RiskFactorResult.raw("ip", 10, Map.of())));

        authenticator(loginContext(context.user), score, AuthAction.ALLOW, new TestOtpAuthenticator()).authenticate(context.proxy);

        assertThat(context.success, equalTo(true));
        assertThat(context.event.getEvent().getDetails().get(RiskScoreAuthenticator.EVENT_RISK_SCORE), equalTo("12"));
        assertThat(context.event.getEvent().getDetails().get(RiskScoreAuthenticator.EVENT_RISK_LEVEL), equalTo("LOW"));
        assertThat(context.event.getEvent().getDetails().get(RiskScoreAuthenticator.EVENT_RISK_ACTION), equalTo("ALLOW"));
        assertThat(context.event.getEvent().getDetails().get(RiskScoreAuthenticator.EVENT_RISK_FACTORS),
                equalTo("failed_attempts:0,ip:10"));
        assertThat(context.event.getEvent().getDetails().get(LoginContextCollector.EVENT_DETAIL_DEVICE), notNullValue());
        assertThat(context.event.getEvent().getDetails().get(LoginContextCollector.EVENT_DETAIL_GEO), equalTo("US"));
    }

    @Test
    public void mediumRiskSetsNotesAndChallengesForOtp() {
        TestContext context = new TestContext(user("user-1"));
        TestOtpAuthenticator otp = new TestOtpAuthenticator();

        authenticator(loginContext(context.user), score(40, RiskLevel.MEDIUM, List.of()), AuthAction.STEP_UP_MFA, otp)
                .authenticate(context.proxy);

        assertThat(otp.authenticateCalled, equalTo(true));
        assertThat(context.authNotes.get(RiskScoreAuthenticator.RISK_STEP_UP), equalTo("true"));
        assertThat(context.authNotes.get(RiskScoreAuthenticator.RISK_LEVEL), equalTo("MEDIUM"));
        assertThat(context.authNotes.get(RiskScoreAuthenticator.RISK_SCORE), equalTo("40"));
    }

    @Test
    public void mediumRiskWithoutUserBlocksInsteadOfCrashing() {
        TestContext context = new TestContext(null);

        authenticator(loginContext(null), score(40, RiskLevel.MEDIUM, List.of()), AuthAction.STEP_UP_MFA,
                new TestOtpAuthenticator()).authenticate(context.proxy);

        assertThat(context.failureError, equalTo(AuthenticationFlowError.ACCESS_DENIED));
        assertThat(context.failureResponse.getStatus(), equalTo(Response.Status.UNAUTHORIZED.getStatusCode()));
    }

    @Test
    public void mediumRiskWithoutConfiguredOtpBlocksInsteadOfBypassingStepUp() {
        TestContext context = new TestContext(user("user-1"));
        TestOtpAuthenticator otp = new TestOtpAuthenticator(false);

        authenticator(loginContext(context.user), score(40, RiskLevel.MEDIUM, List.of()), AuthAction.STEP_UP_MFA, otp)
                .authenticate(context.proxy);

        assertThat(otp.authenticateCalled, equalTo(false));
        assertThat(context.failureError, equalTo(AuthenticationFlowError.ACCESS_DENIED));
        assertThat(context.failureResponse.getStatus(), equalTo(Response.Status.UNAUTHORIZED.getStatusCode()));
    }

    @Test
    public void mediumRiskActionDelegatesToOtpValidation() {
        TestContext context = new TestContext(user("user-1"));
        context.authNotes.put(RiskScoreAuthenticator.RISK_STEP_UP, "true");
        TestOtpAuthenticator otp = new TestOtpAuthenticator();

        authenticator(loginContext(context.user), score(40, RiskLevel.MEDIUM, List.of()), AuthAction.STEP_UP_MFA, otp)
                .action(context.proxy);

        assertThat(otp.actionCalled, equalTo(true));
    }

    @Test
    public void highRiskBlocksWithBrowserChallenge() {
        TestContext context = new TestContext(user("user-1"));

        authenticator(loginContext(context.user), score(90, RiskLevel.HIGH, List.of()), AuthAction.BLOCK,
                new TestOtpAuthenticator()).authenticate(context.proxy);

        assertThat(context.failureError, equalTo(AuthenticationFlowError.ACCESS_DENIED));
        assertThat(context.failureResponse.getStatus(), equalTo(Response.Status.UNAUTHORIZED.getStatusCode()));
        assertThat(context.event.getEvent().getError(), equalTo(Errors.ACCESS_DENIED));
    }

    @Test
    public void missingUserDoesNotCrashLowRiskEvaluation() {
        TestContext context = new TestContext(null);

        authenticator(loginContext(null), score(0, RiskLevel.LOW, List.of()), AuthAction.ALLOW, new TestOtpAuthenticator())
                .authenticate(context.proxy);

        assertThat(new RiskScoreAuthenticator().requiresUser(), equalTo(false));
        assertThat(context.success, equalTo(true));
    }

    private static RiskScoreAuthenticator authenticator(LoginContext loginContext, RiskScore score, AuthAction action,
            OTPFormAuthenticator otpAuthenticator) {
        return new RiskScoreAuthenticator(new TestLoginContextCollector(loginContext), new TestRiskScoringService(score),
                new TestDecisionService(action), otpAuthenticator);
    }

    private static RiskScore score(int value, RiskLevel level, List<RiskFactorResult> factors) {
        return new RiskScore(value, level, factors);
    }

    private static LoginContext loginContext(UserModel user) {
        String userId = user == null ? null : user.getId();
        String username = user == null ? null : user.getUsername();
        return new LoginContext("realm", userId, username, "127.0.0.1", "Browser", "en-US", "US", Instant.EPOCH, 0,
                Set.of(), Set.of(), Set.of(), false);
    }

    private static UserModel user(String id) {
        return proxy(UserModel.class, (proxy, method, args) -> switch (method.getName()) {
            case "getId" -> id;
            case "getUsername" -> "alice";
            default -> defaultValue(method.getReturnType());
        });
    }

    private static EventBuilder eventBuilder(RealmModel realm, KeycloakSession session) {
        return new EventBuilder(realm, session, connection()).storeImmediately(false);
    }

    private static ClientConnection connection() {
        return proxy(ClientConnection.class, (proxy, method, args) -> switch (method.getName()) {
            case "getRemoteAddr", "getRemoteHost" -> "127.0.0.1";
            case "getRemotePort", "getLocalPort" -> 0;
            case "getLocalAddr" -> "127.0.0.1";
            default -> defaultValue(method.getReturnType());
        });
    }

    private static RealmModel realm() {
        return proxy(RealmModel.class, (proxy, method, args) -> switch (method.getName()) {
            case "getId", "getName" -> "realm";
            case "isEventsEnabled" -> false;
            case "getEventsListenersStream" -> Stream.<String>empty();
            case "getEnabledEventTypesStream" -> Stream.<String>empty();
            default -> defaultValue(method.getReturnType());
        });
    }

    private static KeycloakSession session() {
        KeycloakSessionFactory factory = proxy(KeycloakSessionFactory.class, (proxy, method, args) -> {
            if ("getProviderFactoriesStream".equals(method.getName())) {
                return Stream.<ProviderFactory<EventListenerProvider>>empty();
            }
            return defaultValue(method.getReturnType());
        });

        TracingProvider tracing = proxy(TracingProvider.class, (proxy, method, args) -> {
            if ("getCurrentSpan".equals(method.getName())) {
                return Span.getInvalid();
            }
            return defaultValue(method.getReturnType());
        });

        return proxy(KeycloakSession.class, (proxy, method, args) -> {
            if ("getKeycloakSessionFactory".equals(method.getName())) {
                return factory;
            }
            if ("getProvider".equals(method.getName()) && args != null && args.length > 0 && args[0] == TracingProvider.class) {
                return tracing;
            }
            if ("getProvider".equals(method.getName())) {
                return null;
            }
            return defaultValue(method.getReturnType());
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> type.getSimpleName() + "Proxy";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> null;
                };
            }
            return handler.invoke(proxy, method, args);
        });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == void.class) {
            return null;
        }
        return 0;
    }

    private static final class TestContext {
        private final RealmModel realm = realm();
        private final KeycloakSession session = session();
        private final EventBuilder event = eventBuilder(realm, session);
        private final Map<String, String> authNotes = new HashMap<>();
        private final UserModel user;
        private final AuthenticationFlowContext proxy;
        private boolean success;
        private AuthenticationFlowError failureError;
        private Response failureResponse;

        private TestContext(UserModel user) {
            this.user = user;
            AuthenticationSessionModel authSession = proxy(AuthenticationSessionModel.class, (proxy, method, args) -> {
                if ("setAuthNote".equals(method.getName())) {
                    authNotes.put((String) args[0], (String) args[1]);
                    return null;
                }
                if ("getAuthNote".equals(method.getName())) {
                    return authNotes.get(args[0]);
                }
                return defaultValue(method.getReturnType());
            });
            LoginFormsProvider form = proxy(LoginFormsProvider.class, (proxy, method, args) -> {
                if ("setError".equals(method.getName())) {
                    return proxy;
                }
                if ("createErrorPage".equals(method.getName())) {
                    return Response.status((Response.Status) args[0]).build();
                }
                return defaultValue(method.getReturnType());
            });

            this.proxy = proxy(AuthenticationFlowContext.class, (proxy, method, args) -> switch (method.getName()) {
                case "getEvent" -> event;
                case "getRealm" -> realm;
                case "getSession" -> session;
                case "getUser" -> user;
                case "getConnection" -> connection();
                case "getAuthenticationSession" -> authSession;
                case "form" -> form;
                case "success" -> {
                    success = true;
                    yield null;
                }
                case "failure" -> {
                    failureError = (AuthenticationFlowError) args[0];
                    failureResponse = args.length > 1 ? (Response) args[1] : null;
                    yield null;
                }
                default -> defaultValue(method.getReturnType());
            });
        }
    }

    private static final class TestLoginContextCollector extends LoginContextCollector {
        private final LoginContext loginContext;

        private TestLoginContextCollector(LoginContext loginContext) {
            this.loginContext = loginContext;
        }

        @Override
        public LoginContext collect(AuthenticationFlowContext context, AdaptiveAuthPolicy policy) {
            return loginContext;
        }
    }

    private static final class TestRiskScoringService extends RiskScoringService {
        private final RiskScore score;

        private TestRiskScoringService(RiskScore score) {
            this.score = score;
        }

        @Override
        public RiskScore score(LoginContext loginContext, AdaptiveAuthPolicy policy) {
            return score;
        }
    }

    private static final class TestDecisionService extends AdaptiveAuthDecisionService {
        private final AuthAction action;

        private TestDecisionService(AuthAction action) {
            this.action = action;
        }

        @Override
        public AuthAction decide(RiskScore score, AdaptiveAuthPolicy policy) {
            return action;
        }
    }

    private static final class TestOtpAuthenticator extends OTPFormAuthenticator {
        private boolean authenticateCalled;
        private boolean actionCalled;
        private final boolean configured;

        private TestOtpAuthenticator() {
            this(true);
        }

        private TestOtpAuthenticator(boolean configured) {
            this.configured = configured;
        }

        @Override
        public void authenticate(AuthenticationFlowContext context) {
            authenticateCalled = true;
        }

        @Override
        public void action(AuthenticationFlowContext context) {
            actionCalled = true;
        }

        @Override
        public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
            return configured;
        }
    }
}
