/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.services.resources.flows;

import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Flows {

    private Flows() {
    }

    public static LoginFormsProvider forms(KeycloakSession session, RealmModel realm, ClientModel client, UriInfo uriInfo, HttpHeaders headers) {
        return session.getProvider(LoginFormsProvider.class).setRealm(realm).setUriInfo(uriInfo).setClient(client).setHttpHeaders(headers);
    }

    public static ErrorFlows errors() {
        return new ErrorFlows();
    }

    public static Response forwardToSecurityFailurePage(KeycloakSession session, RealmModel realm, UriInfo uriInfo, HttpHeaders headers, String message, Object ... parameters) {
        return Flows.forms(session, realm, null, uriInfo, headers).setError(message,parameters).createErrorPage();
    }


}
