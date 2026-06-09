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

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticator;
import org.keycloak.authentication.authenticators.browser.risk.context.LoginContext;
import org.keycloak.authentication.authenticators.browser.risk.context.LoginContextCollector;
import org.keycloak.authentication.authenticators.browser.risk.decision.AdaptiveAuthDecisionService;
import org.keycloak.authentication.authenticators.browser.risk.decision.AdaptiveAuthPolicy;
import org.keycloak.authentication.authenticators.browser.risk.decision.AuthAction;
import org.keycloak.authentication.authenticators.browser.risk.scoring.RiskFactorResult;
import org.keycloak.authentication.authenticators.browser.risk.scoring.RiskScore;
import org.keycloak.authentication.authenticators.browser.risk.scoring.RiskScoringService;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;

public class RiskScoreAuthenticator implements Authenticator {

    public static final String RISK_STEP_UP = "risk.step_up";
    public static final String RISK_LEVEL = "risk.level";
    public static final String RISK_SCORE = "risk.score";

    public static final String EVENT_RISK_SCORE = "risk_score";
    public static final String EVENT_RISK_LEVEL = "risk_level";
    public static final String EVENT_RISK_ACTION = "risk_action";
    public static final String EVENT_RISK_FACTORS = "risk_factors";

    private final LoginContextCollector contextCollector;
    private final RiskScoringService scoringService;
    private final AdaptiveAuthDecisionService decisionService;
    private final OTPFormAuthenticator otpAuthenticator;

    public RiskScoreAuthenticator() {
        this(new LoginContextCollector(), new RiskScoringService(), new AdaptiveAuthDecisionService(),
                new OTPFormAuthenticator());
    }

    RiskScoreAuthenticator(LoginContextCollector contextCollector, RiskScoringService scoringService,
            AdaptiveAuthDecisionService decisionService, OTPFormAuthenticator otpAuthenticator) {
        this.contextCollector = contextCollector;
        this.scoringService = scoringService;
        this.decisionService = decisionService;
        this.otpAuthenticator = otpAuthenticator;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        AdaptiveAuthPolicy policy = AdaptiveAuthPolicy.fromConfig(config(context));
        LoginContext loginContext = contextCollector.collect(context, policy);
        RiskScore riskScore = scoringService.score(loginContext, policy);
        AuthAction action = decisionService.decide(riskScore, policy);

        addEventDetails(context, loginContext, riskScore, action);

        switch (action) {
            case ALLOW -> context.success();
            case STEP_UP_MFA, CHALLENGE -> stepUp(context, riskScore, otpAuthenticator);
            case BLOCK -> block(context);
        }
    }

    private static Map<String, String> config(AuthenticationFlowContext context) {
        AuthenticatorConfigModel model = context.getAuthenticatorConfig();
        return model == null || model.getConfig() == null ? Collections.emptyMap() : model.getConfig();
    }

    private static void addEventDetails(AuthenticationFlowContext context, LoginContext loginContext, RiskScore score,
            AuthAction action) {
        context.getEvent()
                .detail(EVENT_RISK_SCORE, Integer.toString(score.getValue()))
                .detail(EVENT_RISK_LEVEL, score.getLevel().name())
                .detail(EVENT_RISK_ACTION, action.name())
                .detail(EVENT_RISK_FACTORS, factorSummary(score));

        String device = LoginContextCollector.currentDeviceFingerprint(loginContext.getUserAgent(), loginContext.getAcceptLanguage());
        if (device != null) {
            context.getEvent().detail(LoginContextCollector.EVENT_DETAIL_DEVICE, device);
        }
        if (loginContext.getGeoSignal() != null) {
            context.getEvent().detail(LoginContextCollector.EVENT_DETAIL_GEO, loginContext.getGeoSignal());
        }
    }

    static String factorSummary(RiskScore score) {
        return score.getFactors().stream()
                .map(RiskScoreAuthenticator::factorSummary)
                .collect(Collectors.joining(","));
    }

    private static String factorSummary(RiskFactorResult factor) {
        return factor.getFactor() + ":" + factor.getRawScore();
    }

    private static void stepUp(AuthenticationFlowContext context, RiskScore riskScore, OTPFormAuthenticator otpAuthenticator) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        authSession.setAuthNote(RISK_STEP_UP, Boolean.TRUE.toString());
        authSession.setAuthNote(RISK_LEVEL, riskScore.getLevel().name());
        authSession.setAuthNote(RISK_SCORE, Integer.toString(riskScore.getValue()));

        if (context.getUser() == null) {
            block(context);
            return;
        }

        if (otpAuthenticator.configuredFor(context.getSession(), context.getRealm(), context.getUser())) {
            otpAuthenticator.authenticate(context);
            return;
        }

        block(context);
    }

    private static void block(AuthenticationFlowContext context) {
        context.getEvent().event(EventType.LOGIN).error(Errors.ACCESS_DENIED);
        Response challenge = context.form()
                .setError(Messages.ACCESS_DENIED)
                .createErrorPage(Response.Status.UNAUTHORIZED);
        context.failure(AuthenticationFlowError.ACCESS_DENIED, challenge);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        if (Boolean.TRUE.toString().equals(context.getAuthenticationSession().getAuthNote(RISK_STEP_UP))) {
            if (context.getUser() == null) {
                block(context);
                return;
            }
            otpAuthenticator.action(context);
            return;
        }
        authenticate(context);
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {
    }
}
