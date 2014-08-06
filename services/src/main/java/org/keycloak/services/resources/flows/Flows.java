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

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.ClientConnection;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.social.SocialProvider;

import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Flows {

    private Flows() {
    }

    public static LoginFormsProvider forms(KeycloakSession session, RealmModel realm, ClientModel client, UriInfo uriInfo) {
        return session.getProvider(LoginFormsProvider.class).setRealm(realm).setUriInfo(uriInfo).setClient(client);
    }

    public static OAuthFlows oauth(KeycloakSession session, RealmModel realm, HttpRequest request, UriInfo uriInfo, ClientConnection clientConnection, AuthenticationManager authManager,
            TokenManager tokenManager) {
        return new OAuthFlows(session, realm, request, uriInfo, clientConnection, authManager, tokenManager);
    }

    public static SocialRedirectFlows social(RealmModel realm, UriInfo uriInfo, ClientConnection clientConnection, SocialProvider provider) {
        return new SocialRedirectFlows(realm, uriInfo, clientConnection, provider);
    }

    public static ErrorFlows errors() {
        return new ErrorFlows();
    }

}
