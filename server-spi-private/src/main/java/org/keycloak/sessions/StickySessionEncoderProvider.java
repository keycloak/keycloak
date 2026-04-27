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

package org.keycloak.sessions;

import java.util.Objects;

import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface StickySessionEncoderProvider extends Provider {

    /**
     * @return Encoded value to be used as the value of sticky session cookie (AUTH_SESSION_ID cookie)
     * @deprecated Use {@link #encodeSessionId(String, String)} instead.
     */
    @Deprecated(since = "26.5", forRemoval = true)
    default String encodeSessionId(String sessionId) {
        return encodeSessionId(sessionId, sessionId);
    }

    /**
     * Encodes the route into the {@code message}.
     * <p>
     * The route is computed by the {@code sessionId}, i.e., the Keycloak instance where it is cached.
     *
     * @param message   The message to encode with the route.
     * @param sessionId The session ID stored in the cache.
     * @return The encoded message with the route information.
     * @throws NullPointerException if any parameter is null.
     */
    String encodeSessionId(String message, String sessionId);

    /**
     * @param encodedSessionId value of the sticky session cookie
     * @return decoded value, which represents the actual ID of the {@link AuthenticationSessionModel}
     * @deprecated Use {@link #decodeSessionIdAndRoute(String)} instead.
     */
    @Deprecated(since = "26.5", forRemoval = true)
    default String decodeSessionId(String encodedSessionId) {
        return decodeSessionIdAndRoute(encodedSessionId).sessionId();
    }

    /**
     * Decodes the encoded session ID to extract its components, the session ID and the route.
     * <p>
     * The route component may be {@code null} if the session ID is not correctly encoded, or the sticky session is
     * disabled.
     *
     * @param encodedSessionId The encoded session ID.
     * @return The {@link SessionIdAndRoute} with the session ID and the route component. The route may be {@code null}.
     * @throws NullPointerException if {@code encodeSessionId} is {@code null}.
     */
    SessionIdAndRoute decodeSessionIdAndRoute(String encodedSessionId);

    /**
     * @return true if information about route should be attached to the sticky session cookie by Keycloak. Otherwise,
     * it may be attached by load balancer.
     */
    boolean shouldAttachRoute();

    /**
     * @param sessionId The session ID.
     * @return The route for the session ID. It returns {@code null} if sticky session is disabled.
     * @throws NullPointerException if {@code sessionId} is {@code null}.
     */
    String sessionIdRoute(String sessionId);

    /**
     * @param sessionId The session ID.
     * @param route     The router, i.e., the Keycloak instance where the session is cached. It can be {@code null} if
     *                  sticky session is disabled.
     */
    record SessionIdAndRoute(String sessionId, String route) {

        public SessionIdAndRoute {
            Objects.requireNonNull(sessionId);
        }

        public boolean isSameRoute(String otherRoute) {
            return Objects.equals(otherRoute, route);
        }
    }

}
