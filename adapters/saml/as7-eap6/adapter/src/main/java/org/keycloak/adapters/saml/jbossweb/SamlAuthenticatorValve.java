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

package org.keycloak.adapters.saml.jbossweb;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.deploy.LoginConfig;

import org.keycloak.adapters.jbossweb.JBossWebPrincipalFactory;
import org.keycloak.adapters.saml.*;
import org.keycloak.adapters.spi.SessionIdMapperUpdater;
import org.keycloak.adapters.tomcat.PrincipalFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Keycloak authentication valve
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlAuthenticatorValve extends AbstractSamlAuthenticatorValve {
    public boolean authenticate(Request request, HttpServletResponse response, LoginConfig config) throws java.io.IOException {
        return authenticateInternal(request, response, config);
    }

    @Override
    protected boolean forwardToErrorPageInternal(Request request, HttpServletResponse response, Object loginConfig) throws IOException {
        if (loginConfig == null) return false;
        LoginConfig config = (LoginConfig)loginConfig;
        if (config.getErrorPage() == null) return false;
        forwardToErrorPage(request, (Response)response, config);
        return true;
    }

    @Override
    protected void forwardToLogoutPage(Request request, HttpServletResponse response, SamlDeployment deployment) {
        super.forwardToLogoutPage(request, response, deployment);
    }

    @Override
    public void start() throws LifecycleException {
        StandardContext standardContext = (StandardContext) context;
        standardContext.addLifecycleListener(this);
        super.start();
    }


    public void logout(Request request) {
        logoutInternal(request);
    }

    @Override
    protected PrincipalFactory createPrincipalFactory() {
        return new JBossWebPrincipalFactory();
    }

    @Override
    protected void addTokenStoreUpdaters() {
        context.addApplicationListenerInstance(new IdMapperUpdaterSessionListener(mapper));
        setIdMapperUpdater(SessionIdMapperUpdater.EXTERNAL);
        super.addTokenStoreUpdaters();
    }
}
