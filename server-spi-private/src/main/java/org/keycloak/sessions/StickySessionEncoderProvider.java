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

import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface StickySessionEncoderProvider extends Provider {


    /**
     * @param sessionId
     * @return Encoded value to be used as the value of sticky session cookie (AUTH_SESSION_ID cookie)
     */
    String encodeSessionId(String sessionId);


    /**
     * @param encodedSessionId value of the sticky session cookie
     * @return decoded value, which represents the actual ID of the {@link AuthenticationSessionModel}
     */
    String decodeSessionId(String encodedSessionId);


    /**
     * @return true if information about route should be attached to the sticky session cookie by Keycloak. Otherwise it may be attached by loadbalancer.
     */
    boolean shouldAttachRoute();

}
