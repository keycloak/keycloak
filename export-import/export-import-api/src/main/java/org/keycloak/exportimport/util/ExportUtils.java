package org.keycloak.exportimport.util;

import net.iharder.Base64;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.ApplicationRepresentation;
import org.keycloak.representations.idm.ClaimRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.OAuthClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.ScopeMappingRepresentation;
import org.keycloak.representations.idm.SocialLinkRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ExportUtils {

    public static RealmRepresentation exportRealm(KeycloakSession session, RealmModel realm, boolean includeUsers) {
        RealmRepresentation rep = ModelToRepresentation.toRepresentation(realm, true);

        // Audit
        rep.setEventsEnabled(realm.isEventsEnabled());
        if (realm.getEventsExpiration() != 0) {
            rep.setEventsExpiration(realm.getEventsExpiration());
        }

        if (realm.getEventsListeners() != null) {
            rep.setEventsListeners(new LinkedList<String>(realm.getEventsListeners()));
        }

        // Applications
        List<ApplicationModel> applications = realm.getApplications();
        List<ApplicationRepresentation> appReps = new ArrayList<ApplicationRepresentation>();
        for (ApplicationModel app : applications) {
            ApplicationRepresentation appRep = exportApplication(app);
            appReps.add(appRep);
        }
        rep.setApplications(appReps);

        // OAuth clients
        List<OAuthClientModel> oauthClients = realm.getOAuthClients();
        List<OAuthClientRepresentation> oauthClientReps = new ArrayList<OAuthClientRepresentation>();
        for (OAuthClientModel oauthClient : oauthClients) {
            OAuthClientRepresentation clientRep = ModelToRepresentation.toRepresentation(oauthClient);
            clientRep.setSecret(oauthClient.getSecret());
            oauthClientReps.add(clientRep);
        }
        rep.setOauthClients(oauthClientReps);

        // Roles
        List<RoleRepresentation> realmRoleReps = null;
        Map<String, List<RoleRepresentation>> appRolesReps = new HashMap<String, List<RoleRepresentation>>();

        Set<RoleModel> realmRoles = realm.getRoles();
        if (realmRoles != null && realmRoles.size() > 0) {
            realmRoleReps = exportRoles(realmRoles);
        }
        for (ApplicationModel app : applications) {
            Set<RoleModel> currentAppRoles = app.getRoles();
            List<RoleRepresentation> currentAppRoleReps = exportRoles(currentAppRoles);
            appRolesReps.put(app.getName(), currentAppRoleReps);
        }

        RolesRepresentation rolesRep = new RolesRepresentation();
        if (realmRoleReps != null) {
            rolesRep.setRealm(realmRoleReps);
        }
        if (appRolesReps.size() > 0) {
            rolesRep.setApplication(appRolesReps);
        }
        rep.setRoles(rolesRep);

        // Scopes
        List<ClientModel> allClients = new ArrayList<ClientModel>(applications);
        allClients.addAll(realm.getOAuthClients());
        Map<String, List<ScopeMappingRepresentation>> appScopeReps = new HashMap<String, List<ScopeMappingRepresentation>>();

        for (ClientModel client : allClients) {
            Set<RoleModel> clientScopes = client.getScopeMappings();
            ScopeMappingRepresentation scopeMappingRep = null;
            for (RoleModel scope : clientScopes) {
                if (scope.getContainer() instanceof RealmModel) {
                    if (scopeMappingRep == null) {
                        scopeMappingRep = rep.scopeMapping(client.getClientId());
                    }
                    scopeMappingRep.role(scope.getName());
                } else {
                    ApplicationModel app = (ApplicationModel)scope.getContainer();
                    String appName = app.getName();
                    List<ScopeMappingRepresentation> currentAppScopes = appScopeReps.get(appName);
                    if (currentAppScopes == null) {
                        currentAppScopes = new ArrayList<ScopeMappingRepresentation>();
                        appScopeReps.put(appName, currentAppScopes);
                    }

                    ScopeMappingRepresentation currentClientScope = null;
                    for (ScopeMappingRepresentation scopeMapping : currentAppScopes) {
                        if (scopeMapping.getClient().equals(client.getClientId())) {
                            currentClientScope = scopeMapping;
                            break;
                        }
                    }
                    if (currentClientScope == null) {
                        currentClientScope = new ScopeMappingRepresentation();
                        currentClientScope.setClient(client.getClientId());
                        currentAppScopes.add(currentClientScope);
                    }
                    currentClientScope.role(scope.getName());
                }
            }
        }

        if (appScopeReps.size() > 0) {
            rep.setApplicationScopeMappings(appScopeReps);
        }

        // Finally users if needed
        if (includeUsers) {
            List<UserModel> allUsers = session.users().getUsers(realm);
            List<UserRepresentation> users = new ArrayList<UserRepresentation>();
            for (UserModel user : allUsers) {
                UserRepresentation userRep = exportUser(session, realm, user);
                users.add(userRep);
            }

            if (users.size() > 0) {
                rep.setUsers(users);
            }
        }

        return rep;
    }

    /**
     * Full export of application including claims and secret
     * @param app
     * @return full ApplicationRepresentation
     */
    public static ApplicationRepresentation exportApplication(ApplicationModel app) {
        ApplicationRepresentation appRep = ModelToRepresentation.toRepresentation(app);

        appRep.setSecret(app.getSecret());
        ClaimRepresentation claimRep = ModelToRepresentation.toRepresentation((ClientModel)app);
        appRep.setClaims(claimRep);
        return appRep;
    }

    public static List<RoleRepresentation> exportRoles(Collection<RoleModel> roles) {
        List<RoleRepresentation> roleReps = new ArrayList<RoleRepresentation>();

        for (RoleModel role : roles) {
            RoleRepresentation roleRep = exportRole(role);
            roleReps.add(roleRep);
        }
        return roleReps;
    }

    public static List<String> getRoleNames(Collection<RoleModel> roles) {
        List<String> roleNames = new ArrayList<String>();
        for (RoleModel role : roles) {
            roleNames.add(role.getName());
        }
        return roleNames;
    }

    /**
     * Full export of role including composite roles
     * @param role
     * @return RoleRepresentation with all stuff filled (including composite roles)
     */
    public static RoleRepresentation exportRole(RoleModel role) {
        RoleRepresentation roleRep = ModelToRepresentation.toRepresentation(role);

        Set<RoleModel> composites = role.getComposites();
        if (composites != null && composites.size() > 0) {
            Set<String> compositeRealmRoles = null;
            Map<String, List<String>> compositeAppRoles = null;

            for (RoleModel composite : composites) {
                RoleContainerModel crContainer = composite.getContainer();
                if (crContainer instanceof RealmModel) {

                    if (compositeRealmRoles == null) {
                        compositeRealmRoles = new HashSet<String>();
                    }
                    compositeRealmRoles.add(composite.getName());
                } else {
                    if (compositeAppRoles == null) {
                        compositeAppRoles = new HashMap<String, List<String>>();
                    }

                    ApplicationModel app = (ApplicationModel)crContainer;
                    String appName = app.getName();
                    List<String> currentAppComposites = compositeAppRoles.get(appName);
                    if (currentAppComposites == null) {
                        currentAppComposites = new ArrayList<String>();
                        compositeAppRoles.put(appName, currentAppComposites);
                    }
                    currentAppComposites.add(composite.getName());
                }
            }

            RoleRepresentation.Composites compRep = new RoleRepresentation.Composites();
            if (compositeRealmRoles != null) {
                compRep.setRealm(compositeRealmRoles);
            }
            if (compositeAppRoles != null) {
                compRep.setApplication(compositeAppRoles);
            }

            roleRep.setComposites(compRep);
        }

        return roleRep;
    }

    /**
     * Full export of user (including role mappings and credentials)
     *
     * @param user
     * @return fully exported user representation
     */
    public static UserRepresentation exportUser(KeycloakSession session, RealmModel realm, UserModel user) {
        UserRepresentation userRep = ModelToRepresentation.toRepresentation(user);

        // Social links
        Set<SocialLinkModel> socialLinks = session.users().getSocialLinks(user, realm);
        List<SocialLinkRepresentation> socialLinkReps = new ArrayList<SocialLinkRepresentation>();
        for (SocialLinkModel socialLink : socialLinks) {
            SocialLinkRepresentation socialLinkRep = exportSocialLink(socialLink);
            socialLinkReps.add(socialLinkRep);
        }
        if (socialLinkReps.size() > 0) {
            userRep.setSocialLinks(socialLinkReps);
        }

        // Role mappings
        Set<RoleModel> roles = user.getRoleMappings();
        List<String> realmRoleNames = new ArrayList<String>();
        Map<String, List<String>> appRoleNames = new HashMap<String, List<String>>();
        for (RoleModel role : roles) {
            if (role.getContainer() instanceof RealmModel) {
                realmRoleNames.add(role.getName());
            } else {
                ApplicationModel app = (ApplicationModel)role.getContainer();
                String appName = app.getName();
                List<String> currentAppRoles = appRoleNames.get(appName);
                if (currentAppRoles == null) {
                    currentAppRoles = new ArrayList<String>();
                    appRoleNames.put(appName, currentAppRoles);
                }

                currentAppRoles.add(role.getName());
            }
        }

        if (realmRoleNames.size() > 0) {
            userRep.setRealmRoles(realmRoleNames);
        }
        if (appRoleNames.size() > 0) {
            userRep.setApplicationRoles(appRoleNames);
        }

        // Credentials
        List<UserCredentialValueModel> creds = user.getCredentialsDirectly();
        List<CredentialRepresentation> credReps = new ArrayList<CredentialRepresentation>();
        for (UserCredentialValueModel cred : creds) {
            CredentialRepresentation credRep = exportCredential(cred);
            credReps.add(credRep);
        }
        userRep.setCredentials(credReps);
        userRep.setFederationLink(user.getFederationLink());

        return userRep;
    }

    public static SocialLinkRepresentation exportSocialLink(SocialLinkModel socialLink) {
        SocialLinkRepresentation socialLinkRep = new SocialLinkRepresentation();
        socialLinkRep.setSocialProvider(socialLink.getSocialProvider());
        socialLinkRep.setSocialUserId(socialLink.getSocialUserId());
        socialLinkRep.setSocialUsername(socialLink.getSocialUsername());
        return socialLinkRep;
    }

    public static CredentialRepresentation exportCredential(UserCredentialValueModel userCred) {
        CredentialRepresentation credRep = new CredentialRepresentation();
        credRep.setType(userCred.getType());
        credRep.setDevice(userCred.getDevice());
        credRep.setHashedSaltedValue(userCred.getValue());
        credRep.setSalt(Base64.encodeBytes(userCred.getSalt()));
        credRep.setHashIterations(userCred.getHashIterations());
        return credRep;
    }

    // Streaming API

    public static void exportUsersToStream(KeycloakSession session, RealmModel realm, List<UserModel> usersToExport, ObjectMapper mapper, OutputStream os) throws IOException {
        JsonFactory factory = mapper.getJsonFactory();
        JsonGenerator generator = factory.createJsonGenerator(os, JsonEncoding.UTF8);
        try {
            if (mapper.isEnabled(SerializationConfig.Feature.INDENT_OUTPUT)) {
                generator.useDefaultPrettyPrinter();
            }
            generator.writeStartObject();
            generator.writeStringField("realm", realm.getName());
            // generator.writeStringField("strategy", strategy.toString());
            generator.writeFieldName("users");
            generator.writeStartArray();

            for (UserModel user : usersToExport) {
                UserRepresentation userRep = ExportUtils.exportUser(session, realm, user);
                generator.writeObject(userRep);
            }

            generator.writeEndArray();
            generator.writeEndObject();
        } finally {
            generator.close();
        }
    }
}
