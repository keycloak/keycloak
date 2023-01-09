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

package org.keycloak.adapters.saml.tomcat;

import org.apache.catalina.authenticator.FormAuthenticator;
import org.apache.catalina.connector.Request;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.tomcat.util.descriptor.web.LoginConfig;
import org.keycloak.adapters.saml.AbstractSamlAuthenticatorValve;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.saml.SamlSessionStore;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.tomcat.GenericPrincipalFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.List;

/**
 * Keycloak authentication valve
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlAuthenticatorValve extends AbstractSamlAuthenticatorValve {
    /**
     * Method called by Tomcat &lt; 8.5.5
     */
    @Override
    public boolean authenticate(Request request, HttpServletResponse response) throws IOException {
       return authenticateInternal(request, response, request.getContext().getLoginConfig());
    }

    /**
     * Method called by Tomcat &gt;= 8.5.5
     */
    @Override
    protected boolean doAuthenticate(Request request, HttpServletResponse response) throws IOException {
       return this.authenticate(request, response);
    }

    @Override
    protected boolean forwardToErrorPageInternal(Request request, HttpServletResponse response, Object loginConfig) throws IOException {
        if (loginConfig == null) return false;
        LoginConfig config = (LoginConfig)loginConfig;
        if (config.getErrorPage() == null) return false;
        // had to do this to get around compiler/IDE issues :(
        try {
            Method method = FormAuthenticator.class.getDeclaredMethod("forwardToErrorPage", Request.class, HttpServletResponse.class, LoginConfig.class);
            method.setAccessible(true);
            method.invoke(this, request, response, config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    @Override
    protected void initInternal() {
        StandardContext standardContext = (StandardContext) context;
        standardContext.addLifecycleListener(this);
    }

    @Override
    public void logout(Request request) {
        logoutInternal(request);
    }

    @Override
    protected GenericPrincipalFactory createPrincipalFactory() {
        return new GenericPrincipalFactory() {
            @Override
            protected GenericPrincipal createPrincipal(Principal userPrincipal, List<String> roles) {
                return new GenericPrincipal(userPrincipal.getName(), null, roles, userPrincipal, null);
            }
        };
    }

    @Override
    protected SamlSessionStore createSessionStore(Request request, HttpFacade facade, SamlDeployment resolvedDeployment) {
        SamlSessionStore store;
        store = new TomcatSamlSessionStore(userSessionManagement, createPrincipalFactory(), mapper, request, this, facade, resolvedDeployment);
        return store;
    }

}
