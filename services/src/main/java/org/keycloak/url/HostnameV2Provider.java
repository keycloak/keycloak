/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.url;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.common.enums.SslRequired;
import org.keycloak.models.KeycloakSession;
import org.keycloak.urls.HostnameProvider;
import org.keycloak.urls.UrlType;

import org.jboss.logging.Logger;

import static org.keycloak.common.util.UriUtils.checkUrl;
import static org.keycloak.urls.UrlType.FRONTEND;
import static org.keycloak.utils.StringUtil.isNotBlank;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class HostnameV2Provider implements HostnameProvider {
    private final KeycloakSession session;
    private final String hostname;
    private final URI hostnameUrl;
    private final URI adminUrl;
    private final Boolean backchannelDynamic;
    private static final UrlType defaultUrlType = FRONTEND;
    private static final String INVALID_REQUEST_SCHEME = "Invalid request scheme";

    private final Logger logger = Logger.getLogger(HostnameV2Provider.class);

    public HostnameV2Provider(KeycloakSession session, String hostname, URI hostnameUrl, URI adminUrl, Boolean backchannelDynamic) {
        this.session = session;
        this.hostname = hostname;
        this.hostnameUrl = hostnameUrl;
        this.adminUrl = adminUrl;
        this.backchannelDynamic = backchannelDynamic;
    }

    @Override
    public URI getBaseUri(UriInfo originalUriInfo, UrlType type) {
        UriBuilder builder;

        switch (type) {
            case ADMIN:
                builder = getAdminUriBuilder(originalUriInfo);
                break;
            case LOCAL_ADMIN:
                builder = getBaseUriBuilder(originalUriInfo);
                // This might not be enough if a reverse proxy is used. In that case we might e.g. have wrong local ports in originalUriInfo.
                // However, that would be transparent to us (we don't know the actual server ports in this context AFAIK).
                builder.host("localhost");
                break;
            case BACKEND:
                builder = backchannelDynamic && !isFrontendRequest(originalUriInfo) ? getBaseUriBuilder(originalUriInfo) : getFrontUriBuilder(originalUriInfo);
                break;
            case FRONTEND:
                builder = getFrontUriBuilder(originalUriInfo);
                break;
            default:
                throw new IllegalArgumentException("Unknown URL type");
        }

        URI uri = buildUri(builder);
        validateRequestScheme(uri);
        // sanitize ports
        int normalizedPort = normalizedPort(uri);
        if (normalizedPort != uri.getPort()) {
            builder.port(normalizedPort);
            uri = buildUri(builder);
        }

        return uri;
    }

    private URI buildUri(UriBuilder builder) {
        try {
            return builder.build();
        } catch (RuntimeException e) {
            throwBadRequestIfUriSyntaxException(e);
            throw e;
        }
    }

    private int normalizedPort(URI uri) {
        if ((isHttpScheme(uri.getScheme()) && uri.getPort() == 80) || (isHttpsScheme(uri.getScheme()) && uri.getPort() == 443)) {
            return -1;
        }
        return uri.getPort();
    }

    private void validateRequestScheme(URI uri) {
        if (!isHttpScheme(uri.getScheme()) && !isHttpsScheme(uri.getScheme())) {
            throw new BadRequestException(INVALID_REQUEST_SCHEME);
        }
    }

    private boolean isHttpScheme(String scheme) {
        return "http".equalsIgnoreCase(scheme);
    }

    private boolean isHttpsScheme(String scheme) {
        return "https".equalsIgnoreCase(scheme);
    }

    private boolean isFrontendRequest(UriInfo originalUriInfo) {
        URI frontend = buildUri(getFrontUriBuilder(originalUriInfo));
        URI originalBaseUri = getBaseUri(originalUriInfo);
        return Objects.equals(frontend.getScheme(), originalBaseUri.getScheme()) &&
                Objects.equals(frontend.getHost(), originalBaseUri.getHost()) &&
                frontend.getPort() == normalizedPort(originalBaseUri);
    }

    private UriBuilder getFrontUriBuilder(UriInfo originalUriInfo) {
        UriBuilder builder = getRealmFrontUriBuilder();

        if (builder != null) {
            return builder;
        }

        if (hostnameUrl != null) {
            builder = UriBuilder.fromUri(hostnameUrl);
        }
        else {
            builder = getBaseUriBuilder(originalUriInfo);
            if (hostname != null) {
                builder.host(hostname);
            }
        }
        return builder;
    }

    private URI getBaseUri(UriInfo originalUriInfo) {
        try {
            return originalUriInfo.getBaseUri();
        } catch (RuntimeException e) {
            throwBadRequestIfUriSyntaxException(e);
            throw e;
        }
    }

    private UriBuilder getBaseUriBuilder(UriInfo originalUriInfo) {
        try {
            return originalUriInfo.getBaseUriBuilder();
        } catch (RuntimeException e) {
            throwBadRequestIfUriSyntaxException(e);
            throw e;
        }
    }

    private void throwBadRequestIfUriSyntaxException(RuntimeException e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof URISyntaxException) {
                throw new BadRequestException(INVALID_REQUEST_SCHEME, e);
            }
        }
    }

    private UriBuilder getRealmFrontUriBuilder() {
        return Optional.ofNullable(session)
                .map(s -> s.getContext())
                .map(c -> c.getRealm())
                .map(r -> r.getAttribute("frontendUrl"))
                .filter(url -> isNotBlank(url))
                .filter(url -> {
                    try {
                        // this check is aligned with other Hostname providers to avoid breaking changes; note that checking URL this way is considered insufficient, see e.g. https://stackoverflow.com/a/5965755
                        checkUrl(SslRequired.NONE, url, "Realm frontendUrl");
                    }
                    catch (IllegalArgumentException e) {
                        logger.errorf(e, "Failed to parse realm frontendUrl '%s'. Falling back to global value.", url);
                        return false;
                    }
                    return true;
                })
                .map(UriBuilder::fromUri)
                .orElse(null);
    }

    private UriBuilder getAdminUriBuilder(UriInfo originalUriInfo) {
        return adminUrl != null ? UriBuilder.fromUri(adminUrl) : getFrontUriBuilder(originalUriInfo);
    }

    @Override
    public String getScheme(UriInfo originalUriInfo, UrlType type) {
        return getBaseUri(originalUriInfo, type).getScheme();
    }

    @Override
    public String getScheme(UriInfo originalUriInfo) {
        return getScheme(originalUriInfo, defaultUrlType);
    }

    @Override
    public String getHostname(UriInfo originalUriInfo, UrlType type) {
        return getBaseUri(originalUriInfo, type).getHost();
    }

    @Override
    public String getHostname(UriInfo originalUriInfo) {
        return getHostname(originalUriInfo, defaultUrlType);
    }

    @Override
    public int getPort(UriInfo originalUriInfo, UrlType type) {
        return getBaseUri(originalUriInfo, type).getPort();
    }

    @Override
    public int getPort(UriInfo originalUriInfo) {
        return getPort(originalUriInfo, defaultUrlType);
    }

    @Override
    public String getContextPath(UriInfo originalUriInfo, UrlType type) {
        return getBaseUri(originalUriInfo, type).getPath();
    }

    @Override
    public String getContextPath(UriInfo originalUriInfo) {
        return getContextPath(originalUriInfo, defaultUrlType);
    }

}
