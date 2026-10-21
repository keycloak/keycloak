/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.migration.migrators;

import java.util.Collections;
import java.util.stream.Collectors;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MigrateTo8_0_0  implements Migration {

    public static final ModelVersion VERSION = new ModelVersion("8.0.0");

    private static final Logger LOG = Logger.getLogger(MigrateTo8_0_0.class);

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }

    @Override
    public void migrate(KeycloakSession session) {
        // Perform basic realm migration first (non multi-factor authentication)
        session.realms().getRealmsStream().forEach(this::migrateRealmCommon);
        // Moreover, for multi-factor authentication migrate optional execution of realm flows to subflows
        session.realms().getRealmsStream().forEach(realm -> migrateRealmMFA(realm));
    }

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        migrateRealmCommon(realm);
        // No-additional-op for multi-factor authentication besides the basic migrateRealmCommon() in previous statement
        // Migration of optional authentication executions was already handled in RepresentationToModel.importRealm
    }

    protected void migrateRealmCommon(RealmModel realm) {
        ClientModel adminConsoleClient = realm.getClientByClientId(Constants.ADMIN_CONSOLE_CLIENT_ID);
        if (adminConsoleClient != null) {
            adminConsoleClient.setRootUrl(Constants.AUTH_ADMIN_URL_PROP);
            String adminConsoleBaseUrl = "/admin/" + realm.getName() + "/console/";
            adminConsoleClient.setBaseUrl(adminConsoleBaseUrl);
            adminConsoleClient.setRedirectUris(Collections.singleton(adminConsoleBaseUrl + "*"));
            adminConsoleClient.setWebOrigins(Collections.singleton("+"));
        }

        ClientModel accountClient = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        if (accountClient != null) {
            accountClient.setRootUrl(Constants.AUTH_BASE_URL_PROP);
            String accountClientBaseUrl = "/realms/" + realm.getName() + "/account/";
            accountClient.setBaseUrl(accountClientBaseUrl);
            accountClient.setRedirectUris(Collections.singleton(accountClientBaseUrl + "*"));
        }
    }

    protected void migrateRealmMFA(RealmModel realm) {
        realm.getAuthenticationFlowsStream().collect(Collectors.toList())
                .forEach(authFlow ->
                        realm.getAuthenticationExecutionsStream(authFlow.getId())
                            .filter(exe -> exe.getRequirement() == AuthenticationExecutionModel.Requirement.CONDITIONAL)
                            .collect(Collectors.toList())
                            .forEach(exe -> migrateOptionalAuthenticationExecution(realm, authFlow, exe, true)));
    }

    public static void migrateOptionalAuthenticationExecution(RealmModel realm, AuthenticationFlowModel parentFlow, AuthenticationExecutionModel optionalExecution, boolean updateOptionalExecution) {
        LOG.debugf("Migrating optional execution '%s' of flow '%s' of realm '%s' to subflow", optionalExecution.getAuthenticator(), parentFlow.getAlias(), realm.getName());

        AuthenticationFlowModel conditionalOTP = new AuthenticationFlowModel();
        conditionalOTP.setTopLevel(false);
        conditionalOTP.setBuiltIn(parentFlow.isBuiltIn());
        conditionalOTP.setAlias(parentFlow.getAlias() + " - " + optionalExecution.getAuthenticator() + " - Conditional");
        conditionalOTP.setDescription("Flow to determine if the " + optionalExecution.getAuthenticator() + " authenticator should be used or not.");
        conditionalOTP.setProviderId("basic-flow");
        conditionalOTP = realm.addAuthenticationFlow(conditionalOTP);

        AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
        execution.setParentFlow(parentFlow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.CONDITIONAL);
        execution.setFlowId(conditionalOTP.getId());
        execution.setPriority(optionalExecution.getPriority());
        execution.setAuthenticatorFlow(true);
        realm.addAuthenticatorExecution(execution);

        execution = new AuthenticationExecutionModel();
        execution.setParentFlow(conditionalOTP.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator("conditional-user-configured");
        execution.setPriority(10);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);

        // Move optionalExecution as child of newly created parent flow
        optionalExecution.setParentFlow(conditionalOTP.getId());
        optionalExecution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        optionalExecution.setPriority(20);

        // In case of DB migration, we're updating existing execution, which is already in DB.
        // In case of JSON migration, the execution is not yet in DB and will be added later
        if (updateOptionalExecution) {
            realm.updateAuthenticatorExecution(optionalExecution);
        }
    }
}
