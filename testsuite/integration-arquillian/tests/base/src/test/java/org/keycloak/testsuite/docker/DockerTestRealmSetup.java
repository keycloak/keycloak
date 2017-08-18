package org.keycloak.testsuite.docker;

import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.protocol.docker.DockerAuthV2Protocol;
import org.keycloak.protocol.docker.DockerAuthenticator;
import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class DockerTestRealmSetup {

    private DockerTestRealmSetup() {
    }

    public static RealmRepresentation createRealm(final String realmId) {
        final RealmRepresentation createdRealm = new RealmRepresentation();
        createdRealm.setId(UUID.randomUUID().toString());
        createdRealm.setRealm(realmId);
        createdRealm.setEnabled(true);
        createdRealm.setAuthenticatorConfig(new ArrayList<>());

        return createdRealm;
    }

    public static void configureDockerAuthenticationFlow(final RealmRepresentation dockerRealm, final String authFlowAlais) {
        final AuthenticationFlowRepresentation dockerBasicAuthFlow = new AuthenticationFlowRepresentation();
        dockerBasicAuthFlow.setId(UUID.randomUUID().toString());
        dockerBasicAuthFlow.setAlias(authFlowAlais);
        dockerBasicAuthFlow.setProviderId("basic-flow");
        dockerBasicAuthFlow.setTopLevel(true);
        dockerBasicAuthFlow.setBuiltIn(false);

        final AuthenticationExecutionExportRepresentation dockerBasicAuthExecution = new AuthenticationExecutionExportRepresentation();
        dockerBasicAuthExecution.setAuthenticator(DockerAuthenticator.ID);
        dockerBasicAuthExecution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED.name());
        dockerBasicAuthExecution.setPriority(0);
        dockerBasicAuthExecution.setUserSetupAllowed(false);
        dockerBasicAuthExecution.setAutheticatorFlow(false);

        final List<AuthenticationExecutionExportRepresentation> authenticationExecutions = Optional.ofNullable(dockerBasicAuthFlow.getAuthenticationExecutions()).orElse(new ArrayList<>());
        authenticationExecutions.add(dockerBasicAuthExecution);
        dockerBasicAuthFlow.setAuthenticationExecutions(authenticationExecutions);

        final List<AuthenticationFlowRepresentation> authenticationFlows = Optional.ofNullable(dockerRealm.getAuthenticationFlows()).orElse(new ArrayList<>());
        authenticationFlows.add(dockerBasicAuthFlow);
        dockerRealm.setAuthenticationFlows(authenticationFlows);
        dockerRealm.setBrowserFlow(dockerBasicAuthFlow.getAlias());
    }


    public static void configureDockerRegistryClient(final RealmRepresentation dockerRealm, final String clientId) {
        final ClientRepresentation dockerClient = new ClientRepresentation();
        dockerClient.setClientId(clientId);
        dockerClient.setProtocol(DockerAuthV2Protocol.LOGIN_PROTOCOL);
        dockerClient.setEnabled(true);

        final List<ClientRepresentation> clients = Optional.ofNullable(dockerRealm.getClients()).orElse(new ArrayList<>());
        clients.add(dockerClient);
        dockerRealm.setClients(clients);
    }

    public static void configureUser(final RealmRepresentation dockerRealm, final String username, final String password) {
        final UserRepresentation dockerUser = new UserRepresentation();
        dockerUser.setUsername(username);
        dockerUser.setEnabled(true);
        dockerUser.setEmail("docker-users@localhost.localdomain");
        dockerUser.setFirstName("docker");
        dockerUser.setLastName("user");

        final CredentialRepresentation dockerUserCreds = new CredentialRepresentation();
        dockerUserCreds.setType(CredentialRepresentation.PASSWORD);
        dockerUserCreds.setValue(password);
        dockerUser.setCredentials(Collections.singletonList(dockerUserCreds));

        dockerRealm.setUsers(Collections.singletonList(dockerUser));
    }

}
