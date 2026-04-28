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
package org.keycloak.protocol.oid4vc.model;

import java.util.List;

import org.keycloak.VCFormat;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.mdoc.MdocAlgorithm;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;

final class CredentialSigningAlgorithmResolver {

    private static final Logger LOGGER = Logger.getLogger(CredentialSigningAlgorithmResolver.class);

    private CredentialSigningAlgorithmResolver() {
    }

    static List<String> resolveMetadataSigningAlgorithms(KeycloakSession keycloakSession,
                                                         CredentialScopeModel credentialModel,
                                                         String format,
                                                         List<String> globalSupportedSigningAlgorithms) {
        if (!VCFormat.MSO_MDOC.equals(format)) {
            String signingAlgorithm = credentialModel.getSigningAlg();
            return StringUtil.isBlank(signingAlgorithm)
                    ? List.copyOf(globalSupportedSigningAlgorithms)
                    : List.of(signingAlgorithm);
        }

        String signingAlgorithm = resolveConfiguredMdocSigningAlgorithm(keycloakSession, credentialModel);
        if (StringUtil.isNotBlank(signingAlgorithm)) {
            if (!isSupportedForFormat(format, signingAlgorithm)) {
                LOGGER.warnf("Configured signing algorithm '%s' is unsupported for credential format '%s'.",
                        signingAlgorithm, format);
                return List.of();
            }
            return List.of(signingAlgorithm);
        }

        // OID4VCI 1.0 Appendix A.2.2 advertises mDoc signing algorithms as COSE values for IssuerAuth.
        // Only include realm keys that we can map from Keycloak's JOSE algorithm names to COSE identifiers.
        return resolveAvailableMdocSigningAlgorithms(keycloakSession);
    }

    static String resolveSigningAlgorithm(KeycloakSession keycloakSession,
                                          CredentialScopeModel credentialModel,
                                          String format,
                                          String defaultSigningAlgorithm) {
        if (!VCFormat.MSO_MDOC.equals(format)) {
            return resolveJsonSigningAlgorithm(keycloakSession, credentialModel, defaultSigningAlgorithm);
        }

        String signingAlgorithm = resolveConfiguredMdocSigningAlgorithm(keycloakSession, credentialModel);
        if (StringUtil.isNotBlank(signingAlgorithm)) {
            if (!isSupportedForFormat(format, signingAlgorithm)) {
                LOGGER.warnf("Configured signing algorithm '%s' is unsupported for credential format '%s'.",
                        signingAlgorithm, format);
                return null;
            }
            return signingAlgorithm;
        }

        return resolveAvailableMdocSigningAlgorithms(keycloakSession)
                .stream()
                .findFirst()
                .orElse(null);
    }

    private static String resolveJsonSigningAlgorithm(KeycloakSession keycloakSession,
                                                      CredentialScopeModel credentialModel,
                                                      String defaultSigningAlgorithm) {
        if (StringUtil.isNotBlank(credentialModel.getSigningKeyId())) {
            return keycloakSession.keys()
                    .getKeysStream(keycloakSession.getContext().getRealm())
                    .filter(key -> credentialModel.getSigningKeyId().equals(key.getKid()))
                    .findAny()
                    .map(KeyWrapper::getAlgorithm)
                    .orElse(defaultSigningAlgorithm);
        }

        if (StringUtil.isNotBlank(credentialModel.getSigningAlg())) {
            return credentialModel.getSigningAlg();
        }

        return defaultSigningAlgorithm;
    }

    private static String resolveConfiguredMdocSigningAlgorithm(KeycloakSession keycloakSession,
                                                                CredentialScopeModel credentialModel) {
        if (StringUtil.isNotBlank(credentialModel.getSigningKeyId())) {
            return keycloakSession.keys()
                    .getKeysStream(keycloakSession.getContext().getRealm())
                    .filter(key -> credentialModel.getSigningKeyId().equals(key.getKid()))
                    .findAny()
                    .map(KeyWrapper::getAlgorithm)
                    .orElse(null);
        }

        if (StringUtil.isNotBlank(credentialModel.getSigningAlg())) {
            return credentialModel.getSigningAlg();
        }

        return null;
    }

    private static List<String> resolveAvailableMdocSigningAlgorithms(KeycloakSession keycloakSession) {
        RealmModel realm = keycloakSession.getContext().getRealm();
        return MdocAlgorithm.getSupportedJoseAlgorithms().stream()
                .filter(algorithm -> keycloakSession.keys().getActiveKey(realm, KeyUse.SIG, algorithm) != null)
                .toList();
    }

    private static boolean isSupportedForFormat(String format, String signingAlgorithm) {
        return !VCFormat.MSO_MDOC.equals(format) || MdocAlgorithm.getSupportedJoseAlgorithms().contains(signingAlgorithm);
    }
}
