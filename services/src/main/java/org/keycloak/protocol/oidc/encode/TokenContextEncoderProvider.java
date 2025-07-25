/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.protocol.oidc.encode;

import org.keycloak.models.ClientSessionContext;
import org.keycloak.provider.Provider;

/**
 * Provides ability to encode some context into access token ID, so this information can be later retrieved from the token without the need to use some proprietary/non-standard claims.
 * For example token context can contain info whether it is lightweight access token or regular token, whether it is coming from online session or offline session etc.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface TokenContextEncoderProvider extends Provider {

    AccessTokenContext getTokenContextFromClientSessionContext(ClientSessionContext clientSessionContext, String rawTokenId);

    AccessTokenContext getTokenContextFromTokenId(String encodedTokenId);

    String encodeTokenId(AccessTokenContext tokenContext);
}
