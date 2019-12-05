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

package org.keycloak.authentication.authenticators.browser;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

import javax.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.OtpDecision.ABSTAIN;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.OtpDecision.SHOW_OTP;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.OtpDecision.SKIP_OTP;
import static org.keycloak.models.utils.KeycloakModelUtils.getRoleFromString;

/**
 * An {@link OTPFormAuthenticator} that can conditionally require OTP authentication.
 * <p>
 * <p>
 * The decision for whether or not to require OTP authentication can be made based on multiple conditions
 * which are evaluated in the following order. The first matching condition determines the outcome.
 * </p>
 * <ol>
 * <li>User Attribute</li>
 * <li>Role</li>
 * <li>Request Header</li>
 * <li>Configured Default</li>
 * </ol>
 * <p>
 * If no condition matches, the {@link ConditionalOtpFormAuthenticator} fallback is to require OTP authentication.
 * </p>
 * <p>
 * <h2>User Attribute</h2>
 * A User Attribute like <code>otp_auth</code> can be used to control OTP authentication on individual user level.
 * The supported values are <i>skip</i> and <i>force</i>. If the value is set to <i>skip</i> then the OTP auth is skipped for the user,
 * otherwise if the value is <i>force</i> then the OTP auth is enforced. The setting is ignored for any other value.
 * </p>
 * <p>
 * <h2>Role</h2>
 * A role can be used to control the OTP authentication. If the user has the specified skip OTP role then OTP authentication is skipped for the user.
 * If the user has the specified force OTP role, then the OTP authentication is required for the user.
 * If not configured, e.g.  if no role is selected, then this setting is ignored.
 * <p>
 * </p>
 * <p>
 * <h2>Request Header</h2>
 * <p>
 * Request Headers are matched via regex {@link Pattern}s and can be specified as a whitelist and blacklist.
 * <i>No OTP for Header</i> specifies the pattern for which OTP authentication <b>is not</b> required.
 * This can be used to specify trusted networks, e.g. via: <code>X-Forwarded-Host: (1.2.3.4|1.2.3.5)</code> where
 * The IPs 1.2.3.4, 1.2.3.5 denote trusted machines.
 * <i>Force OTP for Header</i> specifies the pattern for which OTP authentication <b>is</b> required. Whitelist entries take
 * precedence before blacklist entries.
 * </p>
 * <p>
 * <h2>Configured Default</h2>
 * A default fall-though behaviour can be specified to handle cases where all previous conditions did not lead to a conclusion.
 * An OTP authentication is required in case no default is configured.
 * </p>
 *
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class ConditionalOtpFormAuthenticator extends OTPFormAuthenticator {

    public static final String SKIP = "skip";

    public static final String FORCE = "force";

    public static final String OTP_CONTROL_USER_ATTRIBUTE = "otpControlAttribute";

    public static final String SKIP_OTP_ROLE = "skipOtpRole";

    public static final String FORCE_OTP_ROLE = "forceOtpRole";

    public static final String SKIP_OTP_FOR_HTTP_HEADER = "noOtpRequiredForHeaderPattern";

    public static final String FORCE_OTP_FOR_HTTP_HEADER = "forceOtpForHeaderPattern";

    public static final String DEFAULT_OTP_OUTCOME = "defaultOtpOutcome";

    enum OtpDecision {
        SKIP_OTP, SHOW_OTP, ABSTAIN
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {

        Map<String, String> config = context.getAuthenticatorConfig().getConfig();

        if (tryConcludeBasedOn(voteForUserOtpControlAttribute(context.getUser(), config), context)) {
            return;
        }

        if (tryConcludeBasedOn(voteForUserRole(context.getRealm(), context.getUser(), config), context)) {
            return;
        }

        if (tryConcludeBasedOn(voteForHttpHeaderMatchesPattern(context.getHttpRequest().getHttpHeaders().getRequestHeaders(), config), context)) {
            return;
        }

        if (tryConcludeBasedOn(voteForDefaultFallback(config), context)) {
            return;
        }

        showOtpForm(context);
    }

    private OtpDecision voteForDefaultFallback(Map<String, String> config) {

        if (!config.containsKey(DEFAULT_OTP_OUTCOME)) {
            return ABSTAIN;
        }

        switch (config.get(DEFAULT_OTP_OUTCOME)) {
            case SKIP:
                return SKIP_OTP;
            case FORCE:
                return SHOW_OTP;
            default:
                return ABSTAIN;
        }
    }

    private boolean tryConcludeBasedOn(OtpDecision state, AuthenticationFlowContext context) {

        switch (state) {

            case SHOW_OTP:
                showOtpForm(context);
                return true;

            case SKIP_OTP:
                context.success();
                return true;

            default:
                return false;
        }
    }

    private boolean tryConcludeBasedOn(OtpDecision state) {

        switch (state) {

            case SHOW_OTP:
                return true;

            case SKIP_OTP:
                return false;

            default:
                return false;
        }
    }

    private void showOtpForm(AuthenticationFlowContext context) {
        super.authenticate(context);
    }

    private OtpDecision voteForUserOtpControlAttribute(UserModel user, Map<String, String> config) {

        if (!config.containsKey(OTP_CONTROL_USER_ATTRIBUTE)) {
            return ABSTAIN;
        }

        String attributeName = config.get(OTP_CONTROL_USER_ATTRIBUTE);
        if (attributeName == null) {
            return ABSTAIN;
        }

        List<String> values = user.getAttribute(attributeName);

        if (values.isEmpty()) {
            return ABSTAIN;
        }

        String value = values.get(0).trim();

        switch (value) {
            case SKIP:
                return SKIP_OTP;
            case FORCE:
                return SHOW_OTP;
            default:
                return ABSTAIN;
        }
    }

    private OtpDecision voteForHttpHeaderMatchesPattern(MultivaluedMap<String, String> requestHeaders, Map<String, String> config) {

        if (!config.containsKey(FORCE_OTP_FOR_HTTP_HEADER) && !config.containsKey(SKIP_OTP_FOR_HTTP_HEADER)) {
            return ABSTAIN;
        }

        //Inverted to allow white-lists, e.g. for specifying trusted remote hosts: X-Forwarded-Host: (1.2.3.4|1.2.3.5)
        if (containsMatchingRequestHeader(requestHeaders, config.get(SKIP_OTP_FOR_HTTP_HEADER))) {
            return SKIP_OTP;
        }

        if (containsMatchingRequestHeader(requestHeaders, config.get(FORCE_OTP_FOR_HTTP_HEADER))) {
            return SHOW_OTP;
        }

        return ABSTAIN;
    }

    private boolean containsMatchingRequestHeader(MultivaluedMap<String, String> requestHeaders, String headerPattern) {

        if (headerPattern == null) {
            return false;
        }

        //TODO cache RequestHeader Patterns
        //TODO how to deal with pattern syntax exceptions?
        Pattern pattern = Pattern.compile(headerPattern, Pattern.DOTALL);

        for (Map.Entry<String, List<String>> entry : requestHeaders.entrySet()) {

            String key = entry.getKey();

            for (String value : entry.getValue()) {

                String headerEntry = key.trim() + ": " + value.trim();

                if (pattern.matcher(headerEntry).matches()) {
                    return true;
                }
            }
        }

        return false;
    }

    private OtpDecision voteForUserRole(RealmModel realm, UserModel user, Map<String, String> config) {

        if (!config.containsKey(SKIP_OTP_ROLE) && !config.containsKey(FORCE_OTP_ROLE)) {
            return ABSTAIN;
        }

        if (userHasRole(realm, user, config.get(SKIP_OTP_ROLE))) {
            return SKIP_OTP;
        }

        if (userHasRole(realm, user, config.get(FORCE_OTP_ROLE))) {
            return SHOW_OTP;
        }

        return ABSTAIN;
    }

    private boolean userHasRole(RealmModel realm, UserModel user, String roleName) {

        if (roleName == null) {
            return false;
        }

        RoleModel role = getRoleFromString(realm, roleName);
        if (role != null) {
            return user.hasRole(role);
        }
        return false;
    }

    private boolean isOTPRequired(KeycloakSession session, RealmModel realm, UserModel user) {
        MultivaluedMap<String, String> requestHeaders = session.getContext().getRequestHeaders().getRequestHeaders();
        for (AuthenticatorConfigModel configModel : realm.getAuthenticatorConfigs()) {

            if (tryConcludeBasedOn(voteForUserOtpControlAttribute(user, configModel.getConfig()))) {
                return true;
            }
            if (tryConcludeBasedOn(voteForUserRole(realm, user, configModel.getConfig()))) {
                return true;
            }
            if (tryConcludeBasedOn(voteForHttpHeaderMatchesPattern(requestHeaders, configModel.getConfig()))) {
                return true;
            }
            if (configModel.getConfig().get(DEFAULT_OTP_OUTCOME) != null
                    && configModel.getConfig().get(DEFAULT_OTP_OUTCOME).equals(FORCE)
                    && configModel.getConfig().size() <= 1) {
                return true;
            }
            if (containsConditionalOtpConfig(configModel.getConfig())
                && voteForUserOtpControlAttribute(user, configModel.getConfig()) == ABSTAIN
                && voteForUserRole(realm, user, configModel.getConfig()) == ABSTAIN
                && voteForHttpHeaderMatchesPattern(requestHeaders, configModel.getConfig()) == ABSTAIN
                && (voteForDefaultFallback(configModel.getConfig()) == SHOW_OTP
                    || voteForDefaultFallback(configModel.getConfig()) == ABSTAIN)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsConditionalOtpConfig(Map config) {
        return config.containsKey(OTP_CONTROL_USER_ATTRIBUTE)
            || config.containsKey(SKIP_OTP_ROLE)
            || config.containsKey(FORCE_OTP_ROLE)
            || config.containsKey(SKIP_OTP_FOR_HTTP_HEADER)
            || config.containsKey(FORCE_OTP_FOR_HTTP_HEADER)
            || config.containsKey(DEFAULT_OTP_OUTCOME);
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        if (!isOTPRequired(session, realm, user)) {
            user.removeRequiredAction(UserModel.RequiredAction.CONFIGURE_TOTP);
        } else if (!user.getRequiredActions().contains(UserModel.RequiredAction.CONFIGURE_TOTP.name())) {
            user.addRequiredAction(UserModel.RequiredAction.CONFIGURE_TOTP.name());
        }
    }
}
