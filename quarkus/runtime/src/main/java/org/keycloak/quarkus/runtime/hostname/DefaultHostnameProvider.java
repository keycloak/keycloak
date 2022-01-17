/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.hostname;

import static org.keycloak.urls.UrlType.ADMIN;
import static org.keycloak.urls.UrlType.BACKEND;
import static org.keycloak.urls.UrlType.FRONTEND;
import static org.keycloak.utils.StringUtil.isNotBlank;

import java.net.URI;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.Config;
import org.keycloak.common.util.Resteasy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.urls.HostnameProvider;
import org.keycloak.urls.HostnameProviderFactory;
import org.keycloak.urls.UrlType;

public final class DefaultHostnameProvider implements HostnameProvider, HostnameProviderFactory {

    private static final Logger LOGGER = Logger.getLogger(DefaultHostnameProvider.class);

    private static final String REALM_URI_SESSION_ATTRIBUTE = DefaultHostnameProvider.class.getName() + ".realmUrl";

    private String frontChannelHostName;
    private String defaultPath;
    private String defaultHttpScheme;
    private int defaultTlsPort;
    private boolean noProxy;
    private String adminHostName;
    private Boolean strictBackChannel;
    private boolean hostnameEnabled;

    @Override
    public String getScheme(UriInfo originalUriInfo, UrlType urlType) {
        String scheme = forNonStrictBackChannel(originalUriInfo, urlType, this::getScheme, this::getScheme);

        if (scheme != null) {
            return scheme;
        }

        return fromFrontChannel(originalUriInfo, URI::getScheme, this::getScheme, defaultHttpScheme);
    }

    @Override
    public String getHostname(UriInfo originalUriInfo, UrlType urlType) {
        String hostname = forNonStrictBackChannel(originalUriInfo, urlType, this::getHostname, this::getHostname);

        if (hostname != null) {
            return hostname;
        }

        // admin hostname has precedence over frontchannel
        if (ADMIN.equals(urlType) && adminHostName != null) {
            return adminHostName;
        }

        return fromFrontChannel(originalUriInfo, URI::getHost, this::getHostname, frontChannelHostName);
    }

    @Override
    public String getContextPath(UriInfo originalUriInfo, UrlType urlType) {
        String path = forNonStrictBackChannel(originalUriInfo, urlType, this::getContextPath, this::getContextPath);

        if (path != null) {
            return path;
        }

        if (ADMIN.equals(urlType)) {
            // for admin we always resolve to the request path
            return getContextPath(originalUriInfo);
        }

        return fromFrontChannel(originalUriInfo, URI::getPath, this::getContextPath, defaultPath);
    }

    @Override
    public int getPort(UriInfo originalUriInfo, UrlType urlType) {
        Integer port = forNonStrictBackChannel(originalUriInfo, urlType, this::getPort, this::getPort);

        if (port != null) {
            return port;
        }

        if (hostnameEnabled && !noProxy) {
            // if proxy is enabled and hostname is set, assume the server is exposed using default ports
            return -1;
        }

        return fromFrontChannel(originalUriInfo, URI::getPort, this::getPort, null);
    }

    @Override
    public int getPort(UriInfo originalUriInfo) {
        KeycloakSession session = Resteasy.getContextData(KeycloakSession.class);
        int requestPort = session.getContext().getContextObject(HttpRequest.class).getUri().getBaseUri().getPort();
        return noProxy ? defaultTlsPort : requestPort;
    }

    private <T> T forNonStrictBackChannel(UriInfo originalUriInfo, UrlType urlType,
            BiFunction<UriInfo, UrlType, T> frontEndTypeResolver, Function<UriInfo, T> defaultResolver) {
        if (BACKEND.equals(urlType) && !strictBackChannel) {
            if (isHostFromFrontEndUrl(originalUriInfo)) {
                return frontEndTypeResolver.apply(originalUriInfo, FRONTEND);
            }

            return defaultResolver.apply(originalUriInfo);
        }

        return null;
    }

    private <T> T fromFrontChannel(UriInfo originalUriInfo, Function<URI, T> frontEndTypeResolver, Function<UriInfo, T> defaultResolver,
            T defaultValue) {
        URI frontEndUrl = getRealmFrontEndUrl();

        if (frontEndUrl != null) {
            return frontEndTypeResolver.apply(frontEndUrl);
        }

        return defaultValue == null ? defaultResolver.apply(originalUriInfo) : defaultValue;

    }

    private boolean isHostFromFrontEndUrl(UriInfo originalUriInfo) {
        String requestHost = getHostname(originalUriInfo);
        String frontendUrl = getHostname(originalUriInfo, FRONTEND);

        if (requestHost.equals(frontendUrl)) {
            return true;
        }

        URI realmUrl = getRealmFrontEndUrl();

        return realmUrl != null && requestHost.equals(realmUrl.getHost());
    }

    protected URI getRealmFrontEndUrl() {
        KeycloakSession session = Resteasy.getContextData(KeycloakSession.class);
        URI realmUrl = (URI) session.getAttribute(REALM_URI_SESSION_ATTRIBUTE);

        if (realmUrl == null) {
            RealmModel realm = session.getContext().getRealm();

            if (realm != null) {
                String frontendUrl = realm.getAttribute("frontendUrl");

                if (isNotBlank(frontendUrl)) {
                    realmUrl = URI.create(frontendUrl);
                    session.setAttribute(DefaultHostnameProvider.REALM_URI_SESSION_ATTRIBUTE, realmUrl);
                    return realmUrl;
                }
            }
        }

        return realmUrl;
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "default";
    }

    @Override
    public HostnameProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
        frontChannelHostName = config.get("hostname");

        if (config.getBoolean("strict", false) && frontChannelHostName == null) {
            throw new RuntimeException("Strict hostname resolution configured but no hostname was set");
        }

        hostnameEnabled = frontChannelHostName != null;

        Boolean strictHttps = config.getBoolean("strict-https", false);

        if (strictHttps) {
            defaultHttpScheme = "https";
        }

        defaultPath = config.get("path");
        noProxy = Configuration.getConfigValue("kc.proxy").getValue().equals("none");
        defaultTlsPort = Integer.parseInt(Configuration.getConfigValue("kc.https-port").getValue());
        adminHostName = config.get("admin");
        strictBackChannel = config.getBoolean("strict-backchannel", false);

        LOGGER.infov("Hostname settings: FrontEnd: {0}, Strict HTTPS: {1}, Path: {2}, Strict BackChannel: {3}, Admin: {4}",
                frontChannelHostName == null ? "<request>" : frontChannelHostName,
                strictHttps,
                defaultPath == null ? "<request>" : defaultPath,
                strictBackChannel,
                adminHostName == null ? "<request>" : adminHostName);
    }
}
