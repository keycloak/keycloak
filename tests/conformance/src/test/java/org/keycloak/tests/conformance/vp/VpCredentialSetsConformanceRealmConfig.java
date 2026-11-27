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

package org.keycloak.tests.conformance.vp;

import java.util.Map;

import org.keycloak.broker.oid4vp.OID4VPIdentityProviderConfig;

/**
 * The verifier realm requesting the PID next to an optional second credential, each in a credential
 * set of its own. The suite resolves the single required credential from the credential sets and
 * presents only the PID.
 */
public class VpCredentialSetsConformanceRealmConfig extends VpConformanceRealmConfig {

    static final String REQUESTED_CREDENTIALS_WITH_SETS = """
            {
              "credentials": [
                {
                  "id": "pid_sd_jwt",
                  "format": "dc+sd-jwt",
                  "types": ["urn:eudi:pid:1"],
                  "trustMaterialIdps": ["conformance-issuer-trust"]
                },
                {
                  "id": "membership_sd_jwt",
                  "format": "dc+sd-jwt",
                  "types": ["urn:example:membership:1"],
                  "trustMaterialIdps": ["conformance-issuer-trust"]
                }
              ],
              "credentialSets": [
                { "options": [["pid_sd_jwt"]] },
                { "options": [["membership_sd_jwt"]], "required": false }
              ],
              "principal": { "credential": "pid_sd_jwt", "claim": "given_name" }
            }
            """;

    @Override
    protected Map<String, String> idpConfig() {
        return Map.of(OID4VPIdentityProviderConfig.REQUESTED_CREDENTIALS, REQUESTED_CREDENTIALS_WITH_SETS);
    }
}
