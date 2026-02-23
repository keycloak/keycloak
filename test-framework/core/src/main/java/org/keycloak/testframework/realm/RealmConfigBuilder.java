package org.keycloak.testframework.realm;

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
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.util.Collections;

public class RealmConfigBuilder {

    private final RealmRepresentation rep;

    private RealmConfigBuilder(RealmRepresentation rep) {
        this.rep = rep;
    }

    public static RealmConfigBuilder create() {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setEnabled(true);
        return new RealmConfigBuilder(rep);
    }

    public static RealmConfigBuilder update(RealmRepresentation rep) {
        return new RealmConfigBuilder(rep);
    }

    public RealmConfigBuilder id(String id) {
        rep.setId(id);
        return this;
    }

    public RealmConfigBuilder name(String name) {
        rep.setRealm(name);
        return this;
    }

    public RealmConfigBuilder displayName(String displayName) {
        rep.setDisplayName(displayName);
        return this;
    }

    public RealmConfigBuilder client(ClientRepresentation client) {
        rep.setClients(Collections.combine(rep.getClients(), client));
        return this;
    }

    public ClientConfigBuilder addClient(String clientId) {
        ClientRepresentation client = new ClientRepresentation();
        rep.setClients(Collections.combine(rep.getClients(), client));
        return ClientConfigBuilder.update(client).enabled(true).clientId(clientId);
    }

    public UserConfigBuilder addUser(String username) {
        UserRepresentation user = new UserRepresentation();
        rep.setUsers(Collections.combine(rep.getUsers(), user));
        return UserConfigBuilder.update(user).enabled(true).username(username);
    }

    public GroupConfigBuilder addGroup(String name) {
        GroupRepresentation group = new GroupRepresentation();
        rep.setGroups(Collections.combine(rep.getGroups(), group));
        return GroupConfigBuilder.update(group).name(name);
    }

    public RoleConfigBuilder addRole(String name) {
        if (rep.getRoles() == null) {
            rep.setRoles(new RolesRepresentation());
        }

        RoleRepresentation role = new RoleRepresentation();
        rep.getRoles().setRealm(Collections.combine(rep.getRoles().getRealm(), role));
        return RoleConfigBuilder.update(role).name(name);
    }

    public RoleConfigBuilder addClientRole(String clientName, String roleName) {
        if (rep.getRoles() == null) {
            rep.setRoles(new RolesRepresentation());
        }

        RoleRepresentation role = new RoleRepresentation();
        rep.getRoles().setClient(Collections.combine(rep.getRoles().getClient(), clientName, role));
        return RoleConfigBuilder.update(role).name(roleName);
    }

    public AuthenticationFlowConfigBuilder addAuthenticationFlow(String alias, String description, String providerId, boolean topLevel, boolean builtIn) {
        AuthenticationFlowRepresentation flow = new AuthenticationFlowRepresentation();
        rep.setAuthenticationFlows(Collections.combine(rep.getAuthenticationFlows(), flow));
        return AuthenticationFlowConfigBuilder.update(flow).alias(alias).description(description).providerId(providerId).topLevel(topLevel).builtIn(builtIn);
    }

    public RealmConfigBuilder registrationEmailAsUsername(boolean registrationEmailAsUsername) {
        rep.setRegistrationEmailAsUsername(registrationEmailAsUsername);
        return this;
    }

    public RealmConfigBuilder registrationAllowed(boolean allowed) {
        rep.setRegistrationAllowed(allowed);
        return this;
    }

    public RealmConfigBuilder editUsernameAllowed(boolean allowed) {
        rep.setEditUsernameAllowed(allowed);
        return this;
    }

    public RealmConfigBuilder defaultSignatureAlgorithm(String algorithm) {
        rep.setDefaultSignatureAlgorithm(algorithm);
        return this;
    }

    public RealmConfigBuilder adminPermissionsEnabled(boolean enabled) {
        rep.setAdminPermissionsEnabled(enabled);
        return this;
    }

    public RealmConfigBuilder eventsEnabled(boolean enabled) {
        rep.setEventsEnabled(enabled);
        return this;
    }

    public RealmConfigBuilder adminEventsEnabled(boolean enabled) {
        rep.setAdminEventsEnabled(enabled);
        return this;
    }

    public RealmConfigBuilder adminEventsDetailsEnabled(boolean enabled) {
        rep.setAdminEventsDetailsEnabled(enabled);
        return this;
    }

    public RealmConfigBuilder enabledEventTypes(String... enabledEventTypes) {
        rep.setEnabledEventTypes(Collections.combine(rep.getEnabledEventTypes(), enabledEventTypes));
        return this;
    }

    public RealmConfigBuilder setEnabledEventTypes(String... enabledEventTypes) {
        rep.setEnabledEventTypes(List.of(enabledEventTypes));
        return this;
    }

    public RealmConfigBuilder eventsListeners(String... eventListeners) {
        rep.setEventsListeners(Collections.combine(rep.getEventsListeners(), eventListeners));
        return this;
    }

    public RealmConfigBuilder overwriteEventsListeners(String... eventListeners) {
        rep.setEventsListeners(List.of(eventListeners));
        return this;
    }

    public RealmConfigBuilder eventsExpiration(long eventsExpiration) {
        rep.setEventsExpiration(eventsExpiration);
        return this;
    }

    public RealmConfigBuilder roles(String... roleNames) {
        if (rep.getRoles() == null) {
            rep.setRoles(new RolesRepresentation());
        }
        rep.getRoles().setRealm(Collections.combine(
                rep.getRoles().getRealm(),
                Arrays.stream(roleNames).map(r -> Representations.toRole(r, false))
        ));
        return this;
    }

    public RealmConfigBuilder clientRoles(String client, String... clientRoles) {
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

    public RealmConfigBuilder groups(String... groupsNames) {
        rep.setGroups(Collections.combine(rep.getGroups(), Arrays.stream(groupsNames).map(Representations::toGroup)));
        return this;
    }

    public RealmConfigBuilder defaultGroups(String... groupsNames) {
        rep.setDefaultGroups(Collections.combine(rep.getDefaultGroups(), groupsNames));
        return this;
    }

    public RealmConfigBuilder internationalizationEnabled(boolean enabled) {
        rep.setInternationalizationEnabled(enabled);
        return this;
    }

    public RealmConfigBuilder supportedLocales(String... supportedLocales) {
        rep.setSupportedLocales(Collections.combine(rep.getSupportedLocales(), supportedLocales));
        return this;
    }

    public RealmConfigBuilder defaultLocale(String locale) {
        rep.setDefaultLocale(locale);
        return this;
    }

    public RealmConfigBuilder smtp(String host, int port, String from) {
        Map<String, String> config = new HashMap<>();
        config.put("host", host);
        config.put("port", Integer.toString(port));
        config.put("from", from);
        rep.setSmtpServer(config);
        return this;
    }

    public RealmConfigBuilder organizationsEnabled(boolean enabled) {
        rep.setOrganizationsEnabled(enabled);
        return this;
    }

    public RealmConfigBuilder bruteForceProtected(boolean enabled) {
        rep.setBruteForceProtected(enabled);
        return this;
    }

    public RealmConfigBuilder failureFactor(int count) {
        rep.setFailureFactor(count);
        return this;
    }

    public RealmConfigBuilder duplicateEmailsAllowed(boolean allowed) {
        rep.setDuplicateEmailsAllowed(allowed);
        return this;
    }

    public RealmConfigBuilder sslRequired(String sslRequired) {
        rep.setSslRequired(sslRequired);
        return this;
    }

    public RealmConfigBuilder identityProvider(IdentityProviderRepresentation identityProvider) {
        rep.addIdentityProvider(identityProvider);
        return this;
    }

    public RealmConfigBuilder setRememberMe(boolean enabled) {
        rep.setRememberMe(enabled);
        return this;
    }

    public RealmConfigBuilder resetPasswordAllowed(boolean allowed) {
        rep.setResetPasswordAllowed(allowed);
        return this;
    }

    public RealmConfigBuilder clientPolicy(ClientPolicyRepresentation clienPolicyRep) {
        ClientPoliciesRepresentation clientPolicies = rep.getParsedClientPolicies();
        if (clientPolicies == null) {
            clientPolicies = new ClientPoliciesRepresentation();
        }
        List<ClientPolicyRepresentation> policies = clientPolicies.getPolicies();
        policies.add(clienPolicyRep);
        rep.setParsedClientPolicies(clientPolicies);
        return this;
    }

    public RealmConfigBuilder clientProfile(ClientProfileRepresentation clientProfileRep) {
        ClientProfilesRepresentation clientProfiles = rep.getParsedClientProfiles();
        if (clientProfiles == null) {
            clientProfiles = new ClientProfilesRepresentation();
        }
        List<ClientProfileRepresentation> profiles = clientProfiles.getProfiles();
        profiles.add(clientProfileRep);
        rep.setParsedClientProfiles(clientProfiles);
        return this;
    }

    public RealmConfigBuilder browserFlow(String browserFlow) {
        rep.setBrowserFlow(browserFlow);
        return this;
    }

    public RealmConfigBuilder requiredAction(RequiredActionProviderRepresentation requiredAction) {
        rep.setRequiredActions(Collections.combine(rep.getRequiredActions(), requiredAction));
        return this;
    }

    public RealmConfigBuilder webAuthnPolicySignatureAlgorithms(List<String> algorithms) {
        rep.setWebAuthnPolicySignatureAlgorithms(algorithms);
        return this;
    }

    public RealmConfigBuilder webAuthnPolicyAttestationConveyancePreference(String preference) {
        rep.setWebAuthnPolicyAttestationConveyancePreference(preference);
        return this;
    }

    public RealmConfigBuilder webAuthnPolicyAuthenticatorAttachment(String attachment) {
        rep.setWebAuthnPolicyAuthenticatorAttachment(attachment);
        return this;
    }

    public RealmConfigBuilder webAuthnPolicyRequireResidentKey(String residentKey) {
        rep.setWebAuthnPolicyRequireResidentKey(residentKey);
        return this;
    }

    public RealmConfigBuilder webAuthnPolicyUserVerificationRequirement(String requirement) {
        rep.setWebAuthnPolicyUserVerificationRequirement(requirement);
        return this;
    }

    public RealmConfigBuilder webAuthnPolicyRpEntityName(String entityName) {
        rep.setWebAuthnPolicyRpEntityName(entityName);
        return this;
    }

    public RealmConfigBuilder webAuthnPolicyRpId(String rpId) {
        rep.setWebAuthnPolicyRpId(rpId);
        return this;
    }

    public RealmConfigBuilder webAuthnPolicyCreateTimeout(Integer timeout) {
        rep.setWebAuthnPolicyCreateTimeout(timeout);
        return this;
    }

    public RealmConfigBuilder webAuthnPolicyAvoidSameAuthenticatorRegister(Boolean register) {
        rep.setWebAuthnPolicyAvoidSameAuthenticatorRegister(register);
        return this;
    }

    public RealmConfigBuilder webAuthnPolicyPasswordlessSignatureAlgorithms(List<String> algorithms) {
        rep.setWebAuthnPolicySignatureAlgorithms(algorithms);
        return this;
    }

    public RealmConfigBuilder webAuthnPolicyPasswordlessAttestationConveyancePreference(String preference) {
        rep.setWebAuthnPolicyPasswordlessAttestationConveyancePreference(preference);
        return this;
    }

    public RealmConfigBuilder webAuthnPolicyPasswordlessAuthenticatorAttachment(String attachment) {
        rep.setWebAuthnPolicyPasswordlessAuthenticatorAttachment(attachment);
        return this;
    }

    public RealmConfigBuilder webAuthnPolicyPasswordlessRequireResidentKey(String residentKey) {
        rep.setWebAuthnPolicyPasswordlessRequireResidentKey(residentKey);
        return this;
    }

    public RealmConfigBuilder webAuthnPolicyPasswordlessUserVerificationRequirement(String requirement) {
        rep.setWebAuthnPolicyPasswordlessUserVerificationRequirement(requirement);
        return this;
    }

    public RealmConfigBuilder webAuthnPolicyPasswordlessRpEntityName(String entityName) {
        rep.setWebAuthnPolicyPasswordlessRpEntityName(entityName);
        return this;
    }

    public RealmConfigBuilder webAuthnPolicyPasswordlessCreateTimeout(Integer timeout) {
        rep.setWebAuthnPolicyPasswordlessCreateTimeout(timeout);
        return this;
    }

    public RealmConfigBuilder webAuthnPolicyPasswordlessAvoidSameAuthenticatorRegister(Boolean register) {
        rep.setWebAuthnPolicyPasswordlessAvoidSameAuthenticatorRegister(register);
        return this;
    }

    public RealmConfigBuilder webAuthnPolicyAcceptableAaguids(List<String> aaguids) {
        rep.setWebAuthnPolicyAcceptableAaguids(aaguids);
        return this;
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
    public RealmConfigBuilder update(RealmUpdate... update) {
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
