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
package org.keycloak.authorization.authzen;

import java.util.Map;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.Urls;
import org.keycloak.urls.UrlType;
import org.keycloak.wellknown.WellKnownProvider;

public class AuthZenWellKnownProvider implements WellKnownProvider {

    private final KeycloakSession session;

    public AuthZenWellKnownProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getConfig() {
        String realmUri = realmUri(session);

        return Map.of(
              "policy_decision_point", realmUri,
              "access_evaluation_endpoint", accessEvaluationEndpoint(realmUri),
              "access_evaluations_endpoint", accessEvaluationsEndpoint(realmUri)
        );
    }

    /**
     * The audience that access tokens must contain to call the AuthZEN endpoints of the current realm.
     */
    public static String audience(KeycloakSession session) {
        return audience(realmUri(session));
    }

    public static String audience(String realmUri) {
        return String.format("%s/%s", realmUri, AuthZenRealmResourceProviderFactory.PROVIDER_ID);
    }

    public static String accessEvaluationEndpoint(String realmUri) {
        return String.format("%s/%s/%s", realmUri, AuthZenRealmResourceProviderFactory.PROVIDER_ID, AuthZen.EVALUATION_PATH);
    }

    public static String accessEvaluationsEndpoint(String realmUri) {
        return String.format("%s/%s/%s", realmUri, AuthZenRealmResourceProviderFactory.PROVIDER_ID, AuthZen.EVALUATIONS_PATH);
    }

    private static String realmUri(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        return Urls.realmIssuer(session.getContext().getUri(UrlType.FRONTEND).getBaseUri(), realm.getName());
    }

    @Override
    public void close() {
    }
}
