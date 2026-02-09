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

package org.keycloak.adapters.saml;

import org.keycloak.adapters.saml.profile.SamlAuthenticationHandler;
import org.keycloak.adapters.saml.profile.ecp.EcpAuthenticationHandler;
import org.keycloak.adapters.saml.profile.webbrowsersso.WebBrowserSsoAuthenticationHandler;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.adapters.spi.HttpFacade;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class SamlAuthenticator {

    protected static Logger log = Logger.getLogger(SamlAuthenticator.class);

    private final SamlAuthenticationHandler handler;

    public SamlAuthenticator(final HttpFacade facade, final SamlDeployment deployment, final SamlSessionStore sessionStore) {
        this.handler = createAuthenticationHandler(facade, deployment, sessionStore);
    }

    public AuthChallenge getChallenge() {
        return this.handler.getChallenge();
    }

    public AuthOutcome authenticate() {
        log.debugf("SamlAuthenticator is using handler [%s]", this.handler);
        return this.handler.handle(new OnSessionCreated() {
            @Override
            public void onSessionCreated(SamlSession samlSession) {
                completeAuthentication(samlSession);
            }
        });
    }

    protected abstract void completeAuthentication(SamlSession samlSession);

    protected SamlAuthenticationHandler createAuthenticationHandler(HttpFacade facade, SamlDeployment deployment, SamlSessionStore sessionStore) {
        if (EcpAuthenticationHandler.canHandle(facade)) {
            return EcpAuthenticationHandler.create(facade, deployment, sessionStore);
        }

        // defaults to the web browser sso profile
        return createBrowserHandler(facade, deployment, sessionStore);
    }

    protected SamlAuthenticationHandler createBrowserHandler(HttpFacade facade, SamlDeployment deployment, SamlSessionStore sessionStore) {
        return WebBrowserSsoAuthenticationHandler.create(facade, deployment, sessionStore);
    }
}