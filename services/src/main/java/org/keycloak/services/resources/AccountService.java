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
package org.keycloak.services.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.resources.flows.Flows;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AccountService {

    private RealmModel realm;

    @Context
    private HttpRequest request;

    public AccountService(RealmModel realm) {
        this.realm = realm;
    }

    @Path("access")
    @GET
    public Response accessPage() {
        return new Transaction<Response>() {
            protected Response callImpl() {
                return Flows.forms(realm, request).forwardToAccess();
            }
        }.call();
    }

    @Path("")
    @GET
    public Response accountPage() {
        return new Transaction<Response>() {
            protected Response callImpl() {
                return Flows.forms(realm, request).forwardToAccount();
            }
        }.call();
    }

    @Path("social")
    @GET
    public Response socialPage() {
        return new Transaction<Response>() {
            protected Response callImpl() {
                return Flows.forms(realm, request).forwardToSocial();
            }
        }.call();
    }

    @Path("totp")
    @GET
    public Response totpPage() {
        return new Transaction<Response>() {
            protected Response callImpl() {
                return Flows.forms(realm, request).forwardToTotp();
            }
        }.call();
    }

    @Path("password")
    @GET
    public Response passwordPage() {
        return new Transaction<Response>() {
            protected Response callImpl() {
                return Flows.forms(realm, request).forwardToPassword();
            }
        }.call();
    }

}
