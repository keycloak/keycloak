package org.keycloak.testframework.realm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientPolicyRepresentation;
import org.keycloak.representations.idm.ClientProfileRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.util.Collections;

public class RealmBuilder {

    private final RealmRepresentation rep;

    private RealmBuilder(RealmRepresentation rep) {
        this.rep = rep;
    }

    public static RealmBuilder create() {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setEnabled(true);
        return new RealmBuilder(rep);
    }

    public static RealmBuilder update(RealmRepresentation rep) {
        return new RealmBuilder(rep);
    }

    public RealmBuilder id(String id) {
        rep.setId(id);
        return this;
    }

    public RealmBuilder name(String name) {
        rep.setRealm(name);
        return this;
    }

    public RealmBuilder displayName(String displayName) {
        rep.setDisplayName(displayName);
        return this;
    }

    public RealmBuilder client(ClientRepresentation client) {
        rep.setClients(Collections.combine(rep.getClients(), client));
        return this;
    }

    public ClientBuilder addClient(String clientId) {
        ClientRepresentation client = new ClientRepresentation();
        rep.setClients(Collections.combine(rep.getClients(), client));
        return ClientBuilder.update(client).enabled(true).clientId(clientId);
    }

    public UserBuilder addUser(String username) {
        UserRepresentation user = new UserRepresentation();
        rep.setUsers(Collections.combine(rep.getUsers(), user));
        return UserBuilder.update(user).enabled(true).username(username);
    }

    public UserBuilder addUser(UserRepresentation user) {
        rep.setUsers(Collections.combine(rep.getUsers(), user));
        return UserBuilder.update(user);
    }

    public GroupBuilder addGroup(String name) {
        GroupRepresentation group = new GroupRepresentation();
        rep.setGroups(Collections.combine(rep.getGroups(), group));
        return GroupBuilder.update(group).name(name);
    }

    public RoleBuilder addRole(String name) {
        RoleRepresentation role = new RoleRepresentation();
        role.setName(name);
        return addRole(role);
    }

    public RoleBuilder addRole(RoleRepresentation roleRepresentation) {
        if (rep.getRoles() == null) {
            rep.setRoles(new RolesRepresentation());
        }

        rep.getRoles().setRealm(Collections.combine(rep.getRoles().getRealm(), roleRepresentation));
        return RoleBuilder.update(roleRepresentation).name(roleRepresentation.getName());
    }

    public RoleBuilder addClientRole(String clientName, String roleName) {
        if (rep.getRoles() == null) {
            rep.setRoles(new RolesRepresentation());
        }

        RoleRepresentation role = new RoleRepresentation();
        rep.getRoles().setClient(Collections.combine(rep.getRoles().getClient(), clientName, role));
        return RoleBuilder.update(role).name(roleName);
    }

    public AuthenticationFlowBuilder addAuthenticationFlow(String alias, String description, String providerId, boolean topLevel, boolean builtIn) {
        AuthenticationFlowRepresentation flow = new AuthenticationFlowRepresentation();
        rep.setAuthenticationFlows(Collections.combine(rep.getAuthenticationFlows(), flow));
        return AuthenticationFlowBuilder.update(flow).alias(alias).description(description).providerId(providerId).topLevel(topLevel).builtIn(builtIn);
    }

    public RealmBuilder registrationEmailAsUsername(boolean registrationEmailAsUsername) {
        rep.setRegistrationEmailAsUsername(registrationEmailAsUsername);
        return this;
    }

    public RealmBuilder registrationAllowed(boolean allowed) {
        rep.setRegistrationAllowed(allowed);
        return this;
    }

    public RealmBuilder verifyEmail(boolean verifyEmail) {
        rep.setVerifyEmail(verifyEmail);
        return this;
    }

    public RealmBuilder editUsernameAllowed(boolean allowed) {
        rep.setEditUsernameAllowed(allowed);
        return this;
    }

    public RealmBuilder defaultSignatureAlgorithm(String algorithm) {
        rep.setDefaultSignatureAlgorithm(algorithm);
        return this;
    }

    public RealmBuilder adminPermissionsEnabled(boolean enabled) {
        rep.setAdminPermissionsEnabled(enabled);
        return this;
    }

    public RealmBuilder eventsEnabled(boolean enabled) {
        rep.setEventsEnabled(enabled);
        return this;
    }

    public RealmBuilder adminEventsEnabled(boolean enabled) {
        rep.setAdminEventsEnabled(enabled);
        return this;
    }

    public RealmBuilder adminEventsDetailsEnabled(boolean enabled) {
        rep.setAdminEventsDetailsEnabled(enabled);
        return this;
    }

    public RealmBuilder enabledEventTypes(String... enabledEventTypes) {
        rep.setEnabledEventTypes(Collections.combine(rep.getEnabledEventTypes(), enabledEventTypes));
        return this;
    }

    public RealmBuilder setEnabledEventTypes(String... enabledEventTypes) {
        rep.setEnabledEventTypes(List.of(enabledEventTypes));
        return this;
    }

    public RealmBuilder eventsListeners(String... eventListeners) {
        rep.setEventsListeners(Collections.combine(rep.getEventsListeners(), eventListeners));
        return this;
    }

    public RealmBuilder overwriteEventsListeners(String... eventListeners) {
        rep.setEventsListeners(List.of(eventListeners));
        return this;
    }

    public RealmBuilder eventsExpiration(long eventsExpiration) {
        rep.setEventsExpiration(eventsExpiration);
        return this;
    }

    public RealmBuilder roles(String... roleNames) {
        if (rep.getRoles() == null) {
            rep.setRoles(new RolesRepresentation());
        }
        rep.getRoles().setRealm(Collections.combine(
                rep.getRoles().getRealm(),
                Arrays.stream(roleNames).map(r -> Representations.toRole(r, false))
        ));
        return this;
    }

    public RealmBuilder clientRoles(String client, String... clientRoles) {
        if (rep.getRoles() == null) {
            rep.setRoles(new RolesRepresentation());
        }
        rep.getRoles().setClient(Collections.combine(
                rep.getRoles().getClient(),
                client,
                Arrays.stream(clientRoles).map(r -> Representations.toRole(r, true))
        ));
        return this;
    }

    public RealmBuilder groups(String... groupsNames) {
        rep.setGroups(Collections.combine(rep.getGroups(), Arrays.stream(groupsNames).map(Representations::toGroup)));
        return this;
    }

    public RealmBuilder defaultGroups(String... groupsNames) {
        rep.setDefaultGroups(Collections.combine(rep.getDefaultGroups(), groupsNames));
        return this;
    }

    public RealmBuilder internationalizationEnabled(boolean enabled) {
        rep.setInternationalizationEnabled(enabled);
        return this;
    }

    public RealmBuilder supportedLocales(String... supportedLocales) {
        rep.setSupportedLocales(Collections.combine(rep.getSupportedLocales(), supportedLocales));
        return this;
    }

    public RealmBuilder defaultLocale(String locale) {
        rep.setDefaultLocale(locale);
        return this;
    }

    public RealmBuilder smtp(String host, int port, String from) {
        Map<String, String> config = new HashMap<>();
        config.put("host", host);
        config.put("port", Integer.toString(port));
        config.put("from", from);
        rep.setSmtpServer(config);
        return this;
    }

    public RealmBuilder organizationsEnabled(boolean enabled) {
        rep.setOrganizationsEnabled(enabled);
        return this;
    }

    public RealmBuilder revokeRefreshToken(boolean enabled) {
        rep.setRevokeRefreshToken(enabled);
        return this;
    }

    public RealmBuilder refreshTokenMaxReuse(Integer refreshTokenMaxReuse) {
        rep.setRefreshTokenMaxReuse(refreshTokenMaxReuse);
        return this;
    }

    public RealmBuilder ssoSessionIdleTimeout(Integer ssoSessionIdleTimeout) {
        rep.setSsoSessionIdleTimeout(ssoSessionIdleTimeout);
        return this;
    }

    public RealmBuilder ssoSessionIdleTimeoutRememberMe(Integer ssoSessionIdleTimeoutRememberMe) {
        rep.setSsoSessionIdleTimeoutRememberMe(ssoSessionIdleTimeoutRememberMe);
        return this;
    }

    public RealmBuilder ssoSessionMaxLifespan(Integer ssoSessionMaxLifespan) {
        rep.setSsoSessionMaxLifespan(ssoSessionMaxLifespan);
        return this;
    }

    public RealmBuilder ssoSessionMaxLifespanRememberMe(Integer ssoSessionMaxLifespanRememberMe) {
        rep.setSsoSessionMaxLifespanRememberMe(ssoSessionMaxLifespanRememberMe);
        return this;
    }

    public RealmBuilder clientSessionMaxLifespan(Integer clientSessionMaxLifespan) {
        rep.setClientSessionMaxLifespan(clientSessionMaxLifespan);
        return this;
    }

    public RealmBuilder clientSessionIdleTimeout(Integer clientSessionIdleTimeout) {
        rep.setClientSessionIdleTimeout(clientSessionIdleTimeout);
        return this;
    }

    public RealmBuilder bruteForceProtected(boolean enabled) {
        rep.setBruteForceProtected(enabled);
        return this;
    }

    public RealmBuilder failureFactor(int count) {
        rep.setFailureFactor(count);
        return this;
    }

    public RealmBuilder duplicateEmailsAllowed(boolean allowed) {
        rep.setDuplicateEmailsAllowed(allowed);
        return this;
    }

    public RealmBuilder sslRequired(String sslRequired) {
        rep.setSslRequired(sslRequired);
        return this;
    }

    public RealmBuilder identityProvider(IdentityProviderRepresentation identityProvider) {
        rep.addIdentityProvider(identityProvider);
        return this;
    }

    public RealmBuilder identityProviderMapper(IdentityProviderMapperRepresentation identityProviderMapper) {
        rep.addIdentityProviderMapper(identityProviderMapper);
        return this;
    }

    public RealmBuilder setRememberMe(boolean enabled) {
        rep.setRememberMe(enabled);
        return this;
    }

    public RealmBuilder resetPasswordAllowed(boolean allowed) {
        rep.setResetPasswordAllowed(allowed);
        return this;
    }

    public RealmBuilder resetClientPolicies() {
        rep.setParsedClientPolicies(null);
        return this;
    }

    public RealmBuilder clientPolicy(ClientPolicyRepresentation clientPolicyRep) {
        ClientPoliciesRepresentation clientPolicies = rep.getParsedClientPolicies();
        if (clientPolicies == null) {
            clientPolicies = new ClientPoliciesRepresentation();
        }
        List<ClientPolicyRepresentation> policies = clientPolicies.getPolicies();
        policies.add(clientPolicyRep);
        rep.setParsedClientPolicies(clientPolicies);
        return this;
    }

    public RealmBuilder resetClientProfiles() {
        rep.setParsedClientProfiles(null);
        return this;
    }

    public RealmBuilder clientProfile(ClientProfileRepresentation clientProfileRep) {
        ClientProfilesRepresentation clientProfiles = rep.getParsedClientProfiles();
        if (clientProfiles == null) {
            clientProfiles = new ClientProfilesRepresentation();
        }
        List<ClientProfileRepresentation> profiles = clientProfiles.getProfiles();
        profiles.add(clientProfileRep);
        rep.setParsedClientProfiles(clientProfiles);
        return this;
    }

    public RealmBuilder browserFlow(String browserFlow) {
        rep.setBrowserFlow(browserFlow);
        return this;
    }

    public RealmBuilder requiredAction(RequiredActionProviderRepresentation requiredAction) {
        rep.setRequiredActions(Collections.combine(rep.getRequiredActions(), requiredAction));
        return this;
    }

    public RealmBuilder verifiableCredentialsEnabled(boolean enabled) {
        rep.setVerifiableCredentialsEnabled(enabled);
        return this;
    }

    public RealmBuilder webAuthnPolicySignatureAlgorithms(List<String> algorithms) {
        rep.setWebAuthnPolicySignatureAlgorithms(algorithms);
        return this;
    }

    public RealmBuilder webAuthnPolicyAttestationConveyancePreference(String preference) {
        rep.setWebAuthnPolicyAttestationConveyancePreference(preference);
        return this;
    }

    public RealmBuilder webAuthnPolicyAuthenticatorAttachment(String attachment) {
        rep.setWebAuthnPolicyAuthenticatorAttachment(attachment);
        return this;
    }

    public RealmBuilder webAuthnPolicyRequireResidentKey(String residentKey) {
        rep.setWebAuthnPolicyRequireResidentKey(residentKey);
        return this;
    }

    public RealmBuilder webAuthnPolicyUserVerificationRequirement(String requirement) {
        rep.setWebAuthnPolicyUserVerificationRequirement(requirement);
        return this;
    }

    public RealmBuilder webAuthnPolicyRpEntityName(String entityName) {
        rep.setWebAuthnPolicyRpEntityName(entityName);
        return this;
    }

    public RealmBuilder webAuthnPolicyRpId(String rpId) {
        rep.setWebAuthnPolicyRpId(rpId);
        return this;
    }

    public RealmBuilder webAuthnPolicyCreateTimeout(Integer timeout) {
        rep.setWebAuthnPolicyCreateTimeout(timeout);
        return this;
    }

    public RealmBuilder webAuthnPolicyAvoidSameAuthenticatorRegister(Boolean register) {
        rep.setWebAuthnPolicyAvoidSameAuthenticatorRegister(register);
        return this;
    }

    public RealmBuilder webAuthnPolicyPasswordlessSignatureAlgorithms(List<String> algorithms) {
        rep.setWebAuthnPolicySignatureAlgorithms(algorithms);
        return this;
    }

    public RealmBuilder webAuthnPolicyPasswordlessAttestationConveyancePreference(String preference) {
        rep.setWebAuthnPolicyPasswordlessAttestationConveyancePreference(preference);
        return this;
    }

    public RealmBuilder webAuthnPolicyPasswordlessAuthenticatorAttachment(String attachment) {
        rep.setWebAuthnPolicyPasswordlessAuthenticatorAttachment(attachment);
        return this;
    }

    public RealmBuilder webAuthnPolicyPasswordlessRequireResidentKey(String residentKey) {
        rep.setWebAuthnPolicyPasswordlessRequireResidentKey(residentKey);
        return this;
    }

    public RealmBuilder webAuthnPolicyPasswordlessUserVerificationRequirement(String requirement) {
        rep.setWebAuthnPolicyPasswordlessUserVerificationRequirement(requirement);
        return this;
    }

    public RealmBuilder webAuthnPolicyPasswordlessRpEntityName(String entityName) {
        rep.setWebAuthnPolicyPasswordlessRpEntityName(entityName);
        return this;
    }

    public RealmBuilder webAuthnPolicyPasswordlessCreateTimeout(Integer timeout) {
        rep.setWebAuthnPolicyPasswordlessCreateTimeout(timeout);
        return this;
    }

    public RealmBuilder webAuthnPolicyPasswordlessAvoidSameAuthenticatorRegister(Boolean register) {
        rep.setWebAuthnPolicyPasswordlessAvoidSameAuthenticatorRegister(register);
        return this;
    }

    public RealmBuilder webAuthnPolicyPasswordlessPasskeysEnabled(Boolean enabled) {
        rep.setWebAuthnPolicyPasswordlessPasskeysEnabled(enabled);
        return this;
    }

    public RealmBuilder webAuthnPolicyAcceptableAaguids(List<String> aaguids) {
        rep.setWebAuthnPolicyAcceptableAaguids(aaguids);
        return this;
    }

    public RealmBuilder scimEnabled(boolean enabled) {
        rep.setScimApiEnabled(enabled);
        return this;
    }

    public void attribute(String key, String value) {
        if (rep.getAttributes() == null) {
            rep.setAttributes(new HashMap<>());
        }
        rep.getAttributes().put(key, value);
    }

    public void addClientScope(ClientScopeRepresentation clientScope) {
        if (rep.getClientScopes() == null) {
            rep.setClientScopes(new ArrayList<>());
        }
        rep.getClientScopes().add(clientScope);
    }

    /**
     * Best practice is to use other convenience methods when configuring a realm, but while the framework is under
     * active development there may not be a way to perform all updates required. In these cases this method allows
     * applying any changes to the underlying representation.
     *
     * @param update
     * @return this
     * @deprecated
     */
    public RealmBuilder update(RealmUpdate... update) {
        Arrays.stream(update).forEach(u -> u.update(rep));
        return this;
    }

    public RealmRepresentation build() {
        return rep;
    }

    public interface RealmUpdate {

        void update(RealmRepresentation realm);

    }

}
