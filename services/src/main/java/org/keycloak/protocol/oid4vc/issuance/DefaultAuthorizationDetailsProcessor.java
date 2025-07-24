/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oid4vc.issuance;

import org.keycloak.models.UserSessionModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.protocol.oid4vc.model.AuthorizationDetailResponse;

import java.util.List;
import java.util.Collections;

/**
 * Default implementation of {@link AuthorizationDetailsProcessor} that returns an empty list.
 * This is a no-op implementation for standard RAR (Rich Authorization Requests).
 *
 * @author <a href="mailto:Forkim.Akwichek@adorsys.com">Forkim Akwichek</a>
 */
public class DefaultAuthorizationDetailsProcessor implements AuthorizationDetailsProcessor {
    @Override
    public List<AuthorizationDetailResponse> process(UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        // No-op for standard RAR (Rich Authorization Requests) for now
        return Collections.emptyList();
    }
}
