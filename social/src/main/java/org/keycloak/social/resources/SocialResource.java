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
package org.keycloak.social.resources;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.imageio.spi.ServiceRegistry;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.keycloak.social.IdentityProvider;
import org.keycloak.social.IdentityProviderCallback;
import org.keycloak.social.IdentityProviderState;
import org.keycloak.social.util.UriBuilder;
import org.picketlink.idm.model.Attribute;
import org.picketlink.idm.model.User;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Path("social")
public class SocialResource {

    // TODO This is just temporary - need to either save state variables somewhere they can be flushed after a timeout, or
    // alternatively they could be saved in http session, but that is probably not a good idea
    private static final Map<String, IdentityProviderState> states = new HashMap<String, IdentityProviderState>();

    private static synchronized IdentityProviderState getProviderState(IdentityProvider provider) {
        IdentityProviderState s = states.get(provider.getId());
        if (s == null) {
            s = new IdentityProviderState();
            states.put(provider.getId(), s);
        }
        return s;
    }

    @Context
    private HttpHeaders headers;

    @Context
    private UriInfo uriInfo;

    @GET
    @Path("{application}/callback")
    public Response callback(@PathParam("application") String application) throws URISyntaxException {
        String realm = null; // TODO Get realm for application

        IdentityProviderCallback callback = new IdentityProviderCallback();
        callback.setApplication(application);
        callback.setHeaders(headers);
        callback.setUriInfo(uriInfo);

        Iterator<IdentityProvider> itr = ServiceRegistry.lookupProviders(IdentityProvider.class);

        for (IdentityProvider provider = itr.next(); itr.hasNext();) {
            callback.setProviderState(getProviderState(provider));

            if (provider.isCallbackHandler(callback)) {
                User user = provider.processCallback(callback);
                if (user == null) {
                    break;
                }

                String providerUsername = user.getLoginName();
                String providerUsernameKey = provider.getId() + ".username";

                user.setAttribute(new Attribute<String>(providerUsernameKey, user.getLoginName()));

                User existingUser = getUser(realm, user.getLoginName());

                if (existingUser != null) {
                    user = mergeUser(user, existingUser);

                    updateUser(realm, user);
                } else {
                    if (user.getEmail() != null && getUser(realm, user.getEmail()) == null) {
                        user.setLoginName(user.getEmail());
                    } else if (getUser(realm, user.getLoginName()) != null) {
                        for (int i = 0;; i++) {
                            if (getUser(realm, providerUsername + i) == null) {
                                user.setLoginName(providerUsername + i);
                                break;
                            }
                        }
                    }

                    createUser(realm, user);
                }

                // TODO Get bearer token etc and redirect to application callback url
                URI uri = null;
                return Response.seeOther(uri).build();
            }
        }

        return redirectToLogin(application, "login_failed");
    }

    private void createUser(String realm, User user) {
        // TODO Save user in IDM
    }

    @GET
    @Path("providers")
    public List<IdentityProvider> getProviders() {
        List<IdentityProvider> providers = new LinkedList<IdentityProvider>();
        Iterator<IdentityProvider> itr = ServiceRegistry.lookupProviders(IdentityProvider.class);
        while (itr.hasNext()) {
            providers.add(itr.next());
        }
        return providers;
    }

    private User getUser(String realm, String username) {
        // TODO Get user from IDM
        return null;
    }

    private User mergeUser(User source, User destination) {
        if (source.getEmail() != null) {
            destination.setEmail(source.getEmail());
        }

        if (source.getFirstName() != null) {
            destination.setFirstName(source.getFirstName());
        }

        if (source.getLastName() != null) {
            destination.setLastName(source.getLastName());
        }

        for (Attribute<? extends Serializable> attribute : source.getAttributes()) {
            destination.setAttribute(attribute);
        }

        return destination;
    }

    private Response redirectToLogin(String application, String error) {
        URI uri = new UriBuilder(headers, uriInfo, "login?application=" + application + "&error=login_failed").build();
        return Response.seeOther(uri).build();
    }

    @GET
    @Path("{application}/auth/{provider}")
    @Produces(MediaType.TEXT_HTML)
    public Response redirectToProviderAuth(@PathParam("application") String application,
            @PathParam("provider") String providerId) {
        Iterator<IdentityProvider> itr = ServiceRegistry.lookupProviders(IdentityProvider.class);

        IdentityProvider provider;
        for (provider = itr.next(); itr.hasNext() && !provider.getId().equals(providerId);) {
        }

        if (provider == null) {
            return redirectToLogin(application, "invalid_provider");
        }

        IdentityProviderCallback callback = new IdentityProviderCallback();
        callback.setApplication(application);
        callback.setHeaders(headers);
        callback.setUriInfo(uriInfo);
        callback.setProviderKey(null); // TODO Get provider key
        callback.setProviderSecret(null); // TODO Get provider secret

        URI authUrl = provider.getAuthUrl(callback);
        if (authUrl != null) {
            return Response.seeOther(authUrl).build();
        } else {
            return redirectToLogin(application, "invalid_provider");
        }
    }

    private void updateUser(String realm, User user) {
        // TODO Update user in IDM
    }

}
