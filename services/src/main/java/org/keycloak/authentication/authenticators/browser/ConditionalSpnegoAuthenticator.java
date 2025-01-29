/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authentication.authenticators.browser;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * A {@link SpnegoAuthenticator} that can conditionally skip SPNEGO authentication.
 * <p>
 * This Keycloak authenticator allows SPNEGO authentication to be skipped by explicit client request and optionally by
 * request IP. Clients can request SPNEGO authentication be skipped via the URL prompt=login present in the client
 * request. A Whitelist Regex Pattern can optionally be configured, and if configured, it is compared against the
 * <i>X-Forwarded-For</i> header on auth requests to check for a match. If no match then no SPNEGO is skipped.
 * </p>
 * <p>
 * It is often desirable to treat Intranet (internal) users with the convenience of Kerberos Single Sign On, while
 * preventing Internet (external) users from possibly seeing a confusing NTLM prompt dialog.  It can also be useful to
 * allow users to explicitly opt out of SPNEGO in order to provide a switch user functionality.
 * </p>
 * @author <a href="mailto:Ryan.Slominski@gmail.com">Ryan Slominski</a>
 */
public class ConditionalSpnegoAuthenticator extends SpnegoAuthenticator {

    private static Logger logger = Logger.getLogger(ConditionalSpnegoAuthenticator.class);

    public static final String WHITELIST_PATTERN = "XForwardedForWhitelistPattern";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        AuthenticationSessionModel session = context.getAuthenticationSession();
        Map<String, String> clientNotes = session.getClientNotes();
        if ("login".equals(clientNotes.get("prompt"))) {
            logger.debug("Skip SPNEGO because of prompt=login");
            context.attempted();
            return;
        }

        Map<String, String> config = context.getAuthenticatorConfig().getConfig();
        String patternStr = config.get(WHITELIST_PATTERN);
        String xForwardedFor = context.getHttpRequest().getHttpHeaders().getRequestHeaders().getFirst("X-Forwarded-For");

        if(!patternStr.isEmpty()) {
            logger.debug("Matcher pattern: " + patternStr + " ,xForwardedFor: " + xForwardedFor);

            if(xForwardedFor != null && !xForwardedFor.isEmpty()) {
                if (!inWhitelist(patternStr, xForwardedFor)) {
                    logger.debug("Skip SPNEGO because X-Forwarded-For does not match configured pattern");
                    context.attempted();
                    return;
                }
            } else {
                logger.debug("Skip SPNEGO because whitelist pattern defined, but no X-Forwarded-For set");

                context.attempted();
                return;
            }
        }

        super.authenticate(context);
    }

    public boolean inWhitelist(String patternStr, String xForwardedFor) {
        Pattern pattern = Pattern.compile(patternStr);

        return pattern.matcher(xForwardedFor).matches();
    }
}