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
import java.util.Optional;
import java.util.stream.Collectors;

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

        // resolved with the same precedence as the signing path so that the advertised algorithm matches the
        // algorithm actually used to sign the credential
        Optional<String> signingAlgorithm = resolveSupportedConfiguredSigningAlgorithm(
                format,
                resolveConfiguredSigningAlgorithm(keycloakSession, credentialModel, null)
        );
        if (signingAlgorithm.isPresent()) {
            return List.of(signingAlgorithm.get());
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

        Optional<String> signingAlgorithm = resolveSupportedConfiguredSigningAlgorithm(
                format,
                resolveConfiguredSigningAlgorithm(keycloakSession, credentialModel, null)
        );
        if (signingAlgorithm.isPresent()) {
            return signingAlgorithm.get();
        }

        return resolveAvailableMdocSigningAlgorithms(keycloakSession)
                .stream()
                .findFirst()
                .orElse(null);
    }

    private static String resolveJsonSigningAlgorithm(KeycloakSession keycloakSession,
                                                      CredentialScopeModel credentialModel,
                                                      String defaultSigningAlgorithm) {
        return resolveConfiguredSigningAlgorithm(keycloakSession, credentialModel, defaultSigningAlgorithm);
    }

    private static String resolveConfiguredSigningAlgorithm(KeycloakSession keycloakSession,
                                                           CredentialScopeModel credentialModel,
                                                           String fallbackSigningAlgorithm) {
        if (StringUtil.isNotBlank(credentialModel.getSigningKeyId())) {
            return resolveKeyAlgorithm(keycloakSession, credentialModel.getSigningKeyId())
                    .orElse(fallbackSigningAlgorithm);
        }

        if (StringUtil.isNotBlank(credentialModel.getSigningAlg())) {
            return credentialModel.getSigningAlg();
        }

        return fallbackSigningAlgorithm;
    }

    private static Optional<String> resolveSupportedConfiguredSigningAlgorithm(String format, String signingAlgorithm) {
        if (StringUtil.isBlank(signingAlgorithm)) {
            return Optional.empty();
        }

        if (!isSupportedForFormat(format, signingAlgorithm)) {
            LOGGER.warnf("Configured signing algorithm '%s' is unsupported for credential format '%s'.",
                    signingAlgorithm, format);
            return Optional.empty();
        }
        return Optional.of(signingAlgorithm);
    }

    private static Optional<String> resolveKeyAlgorithm(KeycloakSession keycloakSession, String signingKeyId) {
        return keycloakSession.keys()
                .getKeysStream(keycloakSession.getContext().getRealm())
                .filter(key -> signingKeyId.equals(key.getKid()))
                .findAny()
                .map(KeyWrapper::getAlgorithm);
    }

    private static List<String> resolveAvailableMdocSigningAlgorithms(KeycloakSession keycloakSession) {
        RealmModel realm = keycloakSession.getContext().getRealm();
        return keycloakSession.keys().getKeysStream(realm)
                .filter(key -> key.getStatus().isActive())
                .filter(key -> KeyUse.SIG.equals(key.getUse()))
                .map(KeyWrapper::getAlgorithm)
                .filter(MdocAlgorithm.getSupportedJoseAlgorithms()::contains)
                .distinct()
                .collect(Collectors.toList());
    }

    private static boolean isSupportedForFormat(String format, String signingAlgorithm) {
        return !VCFormat.MSO_MDOC.equals(format) || MdocAlgorithm.getSupportedJoseAlgorithms().contains(signingAlgorithm);
    }
}
