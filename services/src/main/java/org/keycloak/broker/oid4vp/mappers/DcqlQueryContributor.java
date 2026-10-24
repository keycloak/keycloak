/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.broker.oid4vp.mappers;

import java.util.List;

import org.keycloak.broker.oid4vp.DcqlQuery;
import org.keycloak.models.IdentityProviderMapperModel;

/**
 * An OID4VP identity provider mapper that reads claims of the presented credential. If no DCQL query
 * is configured on the identity provider, the claims of all its mappers are requested from the wallet.
 */
public interface DcqlQueryContributor {

    /**
     * The claims this mapper reads, empty if it is misconfigured.
     */
    List<DcqlQuery.ClaimsQuery> requestedClaims(IdentityProviderMapperModel mapperModel);
}
