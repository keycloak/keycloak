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

import static org.keycloak.common.util.UriUtils.checkUrl;
import static org.keycloak.config.ProxyOptions.PROXY;
import static org.keycloak.config.ProxyOptions.PROXY_HEADERS;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getConfigValue;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getKcConfigValue;
import static org.keycloak.urls.UrlType.ADMIN;
import static org.keycloak.urls.UrlType.LOCAL_ADMIN;
import static org.keycloak.urls.UrlType.BACKEND;
import static org.keycloak.urls.UrlType.FRONTEND;
import static org.keycloak.utils.StringUtil.isNotBlank;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.function.BiFunction;
import java.util.function.Function;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.config.HostnameV1Options;
import org.keycloak.config.ProxyOptions;
import org.keycloak.config.ProxyOptions.Mode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.urls.HostnameProvider;
import org.keycloak.urls.HostnameProviderFactory;
import org.keycloak.urls.UrlType;
import org.keycloak.utils.KeycloakSessionUtil;

public final class DefaultHostnameProvider implements HostnameProvider, HostnameProviderFactory, EnvironmentDependentProviderFactory {

    private static final Logger LOGGER = Logger.getLogger(DefaultHostnameProvider.class);
    private static final String REALM_URI_SESSION_ATTRIBUTE = DefaultHostnameProvider.class.getName() + ".realmUrl";
    private static final int DEFAULT_HTTPS_PORT_VALUE = 443;
    private static final int RESTEASY_DEFAULT_PORT_VALUE = -1;

    private String frontEndHostName;
    private String defaultPath;
    private String defaultHttpScheme;
    private int defaultTlsPort;
    private boolean noProxy;
    private String adminHostName;
    private Boolean strictBackChannel;
    private boolean hostnameEnabled;
    private boolean strictHttps;
    private int hostnamePort;
    private URI frontEndBaseUri;
    private URI adminBaseUri;
    private URI localAdminUri;

    @Override
    public String getScheme(UriInfo originalUriInfo, UrlType urlType) {
        if (ADMIN.equals(urlType)) {
            return fromBaseUriOrDefault(URI::getScheme, adminBaseUri, getScheme(originalUriInfo));
        }
        if (LOCAL_ADMIN.equals(urlType)) {
            return fromBaseUriOrDefault(URI::getScheme, localAdminUri, getScheme(originalUriInfo));
        }

        String scheme = forNonStrictBackChannel(originalUriInfo, urlType, this::getScheme, this::getScheme);

        if (scheme != null) {
            return scheme;
        }

        return fromFrontEndUrl(originalUriInfo, URI::getScheme, this::getScheme, defaultHttpScheme);
    }

    @Override
    public String getHostname(UriInfo originalUriInfo, UrlType urlType) {
        if (ADMIN.equals(urlType)) {
            return fromBaseUriOrDefault(URI::getHost, adminBaseUri, adminHostName == null ? getHostname(originalUriInfo) : adminHostName);
        }
        if (LOCAL_ADMIN.equals(urlType)) {
            return fromBaseUriOrDefault(URI::getHost, localAdminUri, getHostname(originalUriInfo));
        }

        String hostname = forNonStrictBackChannel(originalUriInfo, urlType, this::getHostname, this::getHostname);

        if (hostname != null) {
            return hostname;
        }

        return fromFrontEndUrl(originalUriInfo, URI::getHost, this::getHostname, frontEndHostName);
    }

    @Override
    public String getContextPath(UriInfo originalUriInfo, UrlType urlType) {
        if (ADMIN.equals(urlType)) {
            return fromBaseUriOrDefault(URI::getPath, adminBaseUri, getContextPath(originalUriInfo));
        }
        if (LOCAL_ADMIN.equals(urlType)) {
            return fromBaseUriOrDefault(URI::getPath, localAdminUri, getContextPath(originalUriInfo));
        }

        String path = forNonStrictBackChannel(originalUriInfo, urlType, this::getContextPath, this::getContextPath);

        if (path != null) {
            return path;
        }

        return fromFrontEndUrl(originalUriInfo, URI::getPath, this::getContextPath, defaultPath);
    }

    @Override
    public int getPort(UriInfo originalUriInfo, UrlType urlType) {
        if (ADMIN.equals(urlType)) {
            return fromBaseUriOrDefault(URI::getPort, adminBaseUri, getRequestPort(originalUriInfo));
        }
        if (LOCAL_ADMIN.equals(urlType)) {
            return fromBaseUriOrDefault(URI::getPort, localAdminUri, getRequestPort(originalUriInfo));
        }

        Integer port = forNonStrictBackChannel(originalUriInfo, urlType, this::getPort, this::getRequestPort);

        if (port != null) {
            return port;
        }

        if (hostnameEnabled && !noProxy) {
            return fromBaseUriOrDefault(URI::getPort, frontEndBaseUri, hostnamePort);
        }

        return fromFrontEndUrl(originalUriInfo, URI::getPort, this::getPort, hostnamePort == -1 ? getPort(originalUriInfo) : hostnamePort);
    }

    @Override
    public int getPort(UriInfo originalUriInfo) {
        return noProxy && strictHttps ? defaultTlsPort : getRequestPort(originalUriInfo);
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

    private <T> T fromFrontEndUrl(UriInfo originalUriInfo, Function<URI, T> frontEndTypeResolver, Function<UriInfo, T> defaultResolver,
            T defaultValue) {
        URI frontEndUrl = getRealmFrontEndUrl();

        if (frontEndUrl != null) {
            return frontEndTypeResolver.apply(frontEndUrl);
        }

        if (frontEndBaseUri != null) {
            return frontEndTypeResolver.apply(frontEndBaseUri);
        }

        return defaultValue == null ? defaultResolver.apply(originalUriInfo) : defaultValue;
    }

    private boolean isHostFromFrontEndUrl(UriInfo originalUriInfo) {
        String requestHost = getHostname(originalUriInfo);
        String frontendUrlHost = getHostname(originalUriInfo, FRONTEND);

        if (requestHost.equals(frontendUrlHost)) {
            return true;
        }

        URI realmUrl = getRealmFrontEndUrl();

        return realmUrl != null && requestHost.equals(realmUrl.getHost());
    }

    protected URI getRealmFrontEndUrl() {
        KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();

        if (session == null) {
            return null;
        }

        RealmModel realm = session.getContext().getRealm();

        if (realm == null) {
            return null;
        }

        String realmUriKey = realm.getId() + REALM_URI_SESSION_ATTRIBUTE;
        URI realmUrl = (URI) session.getAttribute(realmUriKey);

        if (realmUrl == null) {
            String frontendUrl = realm.getAttribute("frontendUrl");

            if (isNotBlank(frontendUrl)) {
                try {
                    checkUrl(SslRequired.NONE, frontendUrl, "frontendUrl");
                    realmUrl = URI.create(frontendUrl);
                    session.setAttribute(realmUriKey, realmUrl);
                    return realmUrl;
                } catch (IllegalArgumentException e) {
                    LOGGER.errorf(e, "Failed to parse realm frontendUrl '%s'. Falling back to global value.", frontendUrl);
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
        boolean isHttpEnabled = Boolean.parseBoolean(getConfigValue("kc.http-enabled").getValue());
        String configPath = getConfigValue("kc.http-relative-path").getValue();

        if (!configPath.startsWith("/")) {
            configPath = "/" + configPath;
        }

        String httpsPort = getConfigValue("kc.https-port").getValue();
        String configPort = isHttpEnabled ? getConfigValue("kc.http-port").getValue() : httpsPort ;

        String scheme = isHttpEnabled ? "http://" : "https://";

        localAdminUri = URI.create(scheme + "localhost:" + configPort + configPath);

        frontEndHostName = config.get("hostname");

        try {
            String url = config.get("hostname-url");

            if (url != null) {
                frontEndBaseUri = new URL(url).toURI();
            }
        } catch (MalformedURLException | URISyntaxException cause) {
            throw new RuntimeException("Invalid base URL for FrontEnd URLs: " + config.get("hostname-url"), cause);
        }

        if (frontEndHostName != null && frontEndBaseUri != null) {
            throw new RuntimeException("You can not set both '" + HostnameV1Options.HOSTNAME.getKey() + "' and '" + HostnameV1Options.HOSTNAME_URL.getKey() + "' options");
        }

        if (config.getBoolean("strict", false) && (frontEndHostName == null && frontEndBaseUri == null)) {
            throw new RuntimeException("Strict hostname resolution configured but no hostname setting provided");
        }

        hostnameEnabled = (frontEndHostName != null || frontEndBaseUri != null);

        if (frontEndBaseUri == null) {
            strictHttps = hostnameEnabled && config.getBoolean("strict-https", false);
        } else {
            frontEndHostName = frontEndBaseUri.getHost();
            strictHttps = "https".equals(frontEndBaseUri.getScheme());
        }

        if (strictHttps) {
            defaultHttpScheme = "https";
        }

        defaultPath = config.get("path", frontEndBaseUri == null ? null : frontEndBaseUri.getPath());

        if (getKcConfigValue(PROXY_HEADERS.getKey()).getValue() != null) { // proxy-headers option was explicitly configured
            noProxy = false;
        } else { // falling back to proxy option
            noProxy = Mode.none.equals(ProxyOptions.Mode.valueOf(getKcConfigValue(PROXY.getKey()).getValue()));
        }

        defaultTlsPort = Integer.parseInt(httpsPort);

        if (defaultTlsPort == DEFAULT_HTTPS_PORT_VALUE) {
            defaultTlsPort = RESTEASY_DEFAULT_PORT_VALUE;
        }

        if (frontEndBaseUri == null) {
            hostnamePort = Integer.parseInt(getConfigValue("kc.hostname-port").getValue());
        } else {
            hostnamePort = frontEndBaseUri.getPort();
        }

        adminHostName = config.get("admin");

        try {
            String url = config.get("admin-url");

            if (url != null) {
                adminBaseUri = new URL(url).toURI();
            }
        } catch (MalformedURLException | URISyntaxException cause) {
            throw new RuntimeException("Invalid base URL for Admin URLs: " + config.get("admin-url"), cause);
        }

        if (adminHostName != null && adminBaseUri != null) {
            throw new RuntimeException("You can not set both '" + HostnameV1Options.HOSTNAME_ADMIN.getKey() + "' and '" + HostnameV1Options.HOSTNAME_ADMIN_URL.getKey() + "' options");
        }

        if (adminBaseUri != null) {
            adminHostName = adminBaseUri.getHost();
        }

        strictBackChannel = config.getBoolean("strict-backchannel", false);

        LOGGER.infov("Hostname settings: Base URL: {0}, Hostname: {1}, Strict HTTPS: {2}, Path: {3}, Strict BackChannel: {4}, Admin URL: {5}, Admin: {6}, Port: {7}, Proxied: {8}",
                frontEndBaseUri == null ? "<unset>" : frontEndBaseUri,
                frontEndHostName == null ? frontEndBaseUri == null ? "<request>" : frontEndBaseUri : frontEndHostName,
                strictHttps,
                defaultPath == null ? "<request>" : "".equals(defaultPath) ? "/" : defaultPath,
                strictBackChannel,
                adminBaseUri == null ? "<unset>" : adminBaseUri,
                adminHostName == null ? adminBaseUri == null ? "<request>" : adminBaseUri : adminHostName,
                String.valueOf(hostnamePort),
                !noProxy);
    }

    private int getRequestPort(UriInfo uriInfo) {
        return uriInfo.getBaseUri().getPort();
    }

    private <T> T fromBaseUriOrDefault(Function<URI, T> resolver, URI baseUri, T defaultValue) {
        if (baseUri != null) {
            return resolver.apply(baseUri);
        }

        return defaultValue;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Feature.HOSTNAME_V1);
    }
}
