package org.keycloak.services.resources.account.resources;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.jboss.resteasy.reactive.NoCache;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.managers.Auth;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.utils.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SshPublicKeyResource {

    public static final String SSH_PUBLIC_KEYS = "sshPublicKeys";
    private final KeycloakSession session;
    private final UserModel user;
    private final RealmModel realm;

    public SshPublicKeyResource(KeycloakSession session, UserModel user) {
        this.session = session;
        this.user = user;
        realm = session.getContext().getRealm();
    }

    @GET
    @Path("/")
    @NoCache
    @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
    public List<String> getSshPublicKeys(){
        return user.getAttributeStream(SSH_PUBLIC_KEYS).collect(Collectors.toList());
    }

    @POST
    @Path("/")
    @NoCache
    public void createSshPublicKeys(String newSshKey){
        List<String> values = user.getAttributeStream(SSH_PUBLIC_KEYS).collect(Collectors.toList());
        values.add(newSshKey);
        user.setAttribute(SSH_PUBLIC_KEYS, values);
    }

    @PUT
    @Path("/")
    @NoCache
    public void createSshPublicKeys(List<String> sshKeys){
        user.setAttribute(SSH_PUBLIC_KEYS, sshKeys);
    }
}
