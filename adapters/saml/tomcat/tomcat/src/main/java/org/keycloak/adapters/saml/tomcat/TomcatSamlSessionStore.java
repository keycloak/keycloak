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

import org.apache.catalina.Session;
import org.apache.catalina.connector.Request;
import org.keycloak.adapters.saml.AbstractSamlAuthenticatorValve;
import org.keycloak.adapters.saml.CatalinaSamlSessionStore;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.SessionIdMapper;
import org.keycloak.adapters.spi.SessionIdMapperUpdater;
import org.keycloak.adapters.tomcat.CatalinaUserSessionManagement;
import org.keycloak.adapters.tomcat.PrincipalFactory;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class TomcatSamlSessionStore extends CatalinaSamlSessionStore {
    public TomcatSamlSessionStore(CatalinaUserSessionManagement sessionManagement, PrincipalFactory principalFactory, SessionIdMapper idMapper, Request request, AbstractSamlAuthenticatorValve valve, HttpFacade facade, SamlDeployment deployment) {
        super(sessionManagement, principalFactory, idMapper, SessionIdMapperUpdater.DIRECT, request, valve, facade, deployment);
    }

    @Override
    protected String changeSessionId(Session session) {
        Request request = this.request;
        if (!deployment.turnOffChangeSessionIdOnLogin()) return request.changeSessionId();
        else return session.getId();
    }
}
