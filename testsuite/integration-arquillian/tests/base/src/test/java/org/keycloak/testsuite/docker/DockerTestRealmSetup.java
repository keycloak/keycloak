package org.keycloak.testsuite.docker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.keycloak.protocol.docker.DockerAuthV2Protocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

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
